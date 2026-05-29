# 拼团锁单压测报告（方案 A · 切片 3）

> 接口：`POST /api/v1/gbm/trade/lock_market_pay_order`（营销 8091）  
> 脚本：`scripts/load/lock_order.js`（k6） / `scripts/load/run-lock-parallel.ps1`（无 k6 备用）  
> 优化：`application-perf.yml` 调大 Tomcat 线程/连接池（**只改这一处**）

---

## 1. 环境与模型

| 项 | 值 |
|----|-----|
| 机器 | 本机 Windows，16 逻辑核（压测时观测） |
| 中间件 | MySQL `13306`、Redis `16379`、RabbitMQ `5672`（docker-compose） |
| 应用 | `group-buy-market-app`，profile `dev`（优化前）/ `dev,perf`（优化后） |
| 并发模型 | 50 并发 worker；优化前 100 次、优化后 150 次（PowerShell Runspace，脚本默认 `-Requests 150`） |
| 活动 | `activityId=100123`，`goodsId=9890001`，每请求独立 `userId` / `outTradeNo` |
| 观测 | Grafana「拼团业务监控」+ Prometheus `gbm_lock_order_*` |

---

## 2. 现象与瓶颈（优化前）

压测时 Grafana / 终端现象：

- **锁单 QPS 低**（约 3 req/s），大量请求耗时顶到 **30s**（客户端超时阈值）。
- `tomcat_threads_busy` 长时间打满；`application-dev.yml` 中 **Tomcat `max-threads=20`、`max-connections=20`、`accept-count=10`**，50 并发下请求在连接器队列排队，属 **线程池/连接数过小**，而非 Redis/MySQL 首要瓶颈。
- 业务上部分返回 `0001`（库存/规则拒绝）仍计入 HTTP 200，不影响「排队导致 P99 变差」的结论。

---

## 3. 优化措施（仅 1 处）

新增 profile **`application-perf.yml`**（不破坏默认 `dev` 启动）：

```yaml
server:
  tomcat:
    max-connections: 200
    threads:
      max: 100
      min-spare: 20
    accept-count: 100
```

**启动优化后实例：**

```powershell
cd f:\实习\s-pay-mall-vs-group-buy\group-buy-market
mvn -DskipTests spring-boot:run -pl group-buy-market-app `
  "-Dspring-boot.run.arguments=--spring.profiles.active=dev,perf"
```

（若 8091 已被占用，可先停旧进程，或加 `--server.port=8092` 并把压测 `BaseUrl` 改为 8092。）

---

## 4. 压测结果

### 4.1 优化前（`dev`，已实测）

命令：

```powershell
& f:\实习\s-pay-mall-vs-group-buy\scripts\load\run-lock-parallel.ps1 `
  -Concurrency 50 -Requests 100 -Label before-tomcat
```

| 指标 | 值 |
|------|-----|
| 总请求 | 100 |
| HTTP 200 | 75 |
| 墙钟 QPS | **3.23** |
| 平均耗时 | **3605 ms** |
| P95 | **30017 ms** |
| **P99** | **30023 ms** |
| Max | 30025 ms |

原始输出：`scripts/load/results-before-tomcat.txt`

### 4.2 优化后（`dev,perf`，已实测 2026-05-28）

命令：

```cmd
f:\实习\s-pay-mall-vs-group-buy\scripts\load\run-lock-parallel.cmd -Label after-tomcat
```

| 指标 | 优化前 (`dev`) | 优化后 (`dev,perf`) | 变化 |
|------|----------------|---------------------|------|
| 总请求 | 100 | 150 | — |
| HTTP 200 | 75 | 119 | — |
| 墙钟 QPS | **3.23** | **4.86** | **+50%** |
| 平均耗时 | **3605 ms** | **2846 ms** | **−21%** |
| P95 | 30017 ms | 30002 ms | 仍顶满 |
| **P99** | **30023 ms** | **30022 ms** | 基本持平 |
| Max | 30025 ms | 30026 ms | 基本持平 |

原始输出：`scripts/load/results-after-tomcat.txt`

**如何解读（面试诚实版）：**

- Tomcat 线程池从 20→100 后，**吞吐和平均延迟明显改善**，说明连接器排队确实是主要矛盾之一。
- **P95/P99 仍约 30s**：压测脚本 `Invoke-WebRequest -TimeoutSec 30`，尾部延迟被**客户端超时上限截断**；同时仍有部分请求在 Redis/MySQL/责任链上接近或触达 30s。
- 结论表述建议：**「QPS 3.2→4.9（+50%），均值 3.6s→2.8s（−21%）；尾延迟需放宽压测超时或继续查 DB 连接池/锁单链路」**，不要只报「P99 从 30s 降到 Xms」。

**k6 复现（可选）：**

```powershell
cd f:\实习\s-pay-mall-vs-group-buy\scripts\load
docker run --rm -i -v "${PWD}/lock_order.js:/scripts/lock_order.js" `
  -e BASE_URL=http://host.docker.internal:8091 `
  -e VUS=50 -e DURATION=60s -e RAMP=20s `
  grafana/k6 run /scripts/lock_order.js
```

---

## 5. 结论（面试可讲）

1. **可观测驱动**：Grafana 看到锁单耗时毛刺 → 压测印证大量请求顶到 30s 超时线。
2. **根因（第一层）**：`dev` 下 Tomcat `max-threads=20`，50 并发排队；`dev,perf` 调大后 **QPS +50%、均值 −21%**。
3. **未完全消除尾延迟**：P99 仍 ~30s，与脚本 30s 超时封顶 + 下游 DB/Redis 耗时叠加有关，可作为「下一步优化」话术（Hikari、压测超时调到 60s 再对比）。
4. **证据链**：`results-before-tomcat.txt` / `results-after-tomcat.txt` + 本表 + Grafana 截图。

---

## 6. 复现清单

- [x] 脚本在仓库 `scripts/load/`
- [x] 优化前数字已落盘
- [x] 优化后：`after-tomcat` 已落盘（QPS 4.86，avg 2846ms）
- [ ] Grafana「锁单耗时」面板截图存入本目录（可选 `docs/perf/grafana-before.png`）
