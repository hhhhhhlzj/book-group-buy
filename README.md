# 书香拼团交易平台

> 双服务 DDD 拼团 + 支付商城：**营销 8091** 负责试算/锁单/结算/退款，**商城 8070** 负责登录/下单/支付宝回调；REST + RabbitMQ + 本地消息表保证跨服务最终一致。  
> 在线演示：<http://111.228.26.5:8070/catalog.html>

[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-brightgreen)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-red)](https://redis.io/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.11-orange)](https://www.rabbitmq.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## 项目演进（可运维改造）

在完整拼团业务闭环之上，我按「**上线后怎么看 → 出问题怎么证 → 怎么修**」补了可观测与演练链路（方案文档见仓库外 `拼团项目-*.md`）：

| 阶段 | 做了什么 | 证据 |
|------|----------|------|
| 可观测基础 | Micrometer + Prometheus 双服务抓取；`X-Trace-Id` 透传；锁单/支付回调业务指标 | [MONITORING.md](group-buy-market/docs/dev-ops/MONITORING.md) |
| 指标驱动优化 | 50 并发压测发现 Tomcat 线程池过小导致 P99 顶满超时；`application-perf` 调连接/线程 | [perf-report.md](docs/perf/perf-report.md) |
| 故障可观测 | 支付回调幂等 + `duplicate` 指标；MQ 停服后 `notify_task` 积压 Gauge + 手动补偿 | [chaos-log.md](docs/chaos/chaos-log.md) |

面试讲稿：[docs/interview/talking-points.md](docs/interview/talking-points.md)

---

## 技术栈

| 类别 | 选型 |
|------|------|
| 语言 / 框架 | Java 17 + Spring Boot 2.7 + MyBatis |
| 中间件 | MySQL 8 / Redis 7 (Redisson) / RabbitMQ 3.11 |
| 可观测 | Micrometer / Prometheus / Grafana |
| 架构 | DDD 分层 / 责任链 / 策略模式 / 本地消息表 |
| 部署 | Docker Compose / Nginx / 京东云 2C4G |

---

## 项目结构

```text
s-pay-mall-vs-group-buy/
├── docs/                         # 压测 / 故障演练 / 面试讲稿
├── scripts/load/                 # 锁单压测脚本
├── scripts/chaos/                # 故障演练脚本
├── group-buy-market/             # 营销服务 :8091
└── s-pay-mall-ddd-market/        # 商城服务 :8070
```

---

## 架构图

```mermaid
graph LR
    U[用户/浏览器]
    M[商城 :8070]
    G[营销 :8091]
    DB[(MySQL)]
    R[(Redis)]
    MQ[(RabbitMQ)]
    P[Prometheus/Grafana]
    AP[支付宝沙箱]

    U --> M
    M -->|REST| G
    M --> AP
    AP -->|回调| M
    M --> MQ
    MQ -.-> G
    G --> DB
    G --> R
    M --> DB
    P -.->|scrape| M
    P -.->|scrape| G
```

---

## 核心亮点

### 1. 拼团锁单 · 三层防超卖

```text
Redis incr  →  setNx  →  MySQL 条件更新（lock_count < target_count）
```

📂 `group-buy-market-domain/.../lock/filter/TeamStockOccupyRuleFilter.java`

### 2. 跨服务一致性 · 本地消息表 + 补偿

```text
支付成功 → notify_task(0) → MQ/HTTP → 失败则 status=2 → Job/手动补偿
```

📂 `TradeRepository.settlementMarketPayOrder`、`TradeTaskService.execNotifyJob`

### 3. 退款 · 策略模式三态

📂 `group-buy-market-domain/.../refund/strategy/`

### 4. 可观测（本次加深）

- **锁单**：`gbm_lock_order_total` / `gbm_lock_order_duration_seconds`
- **支付回调**：`mall_alipay_notify_total{result=success|duplicate}`
- **补偿积压**：`gbm_notify_task_pending`
- **Dashboard**：`group-buy-market/docs/dev-ops/grafana/dashboards/group-buy-business.json`

### 5. 规则树试算 + DCC 动态配置

责任链锁单、Redis Pub/Sub 热更新活动开关与降级策略。

---

## 快速启动

> 敏感配置使用 `application-dev.yml.example` 复制为 `application-dev.yml` 后本地填写，勿提交密钥。

### 1. 中间件

```bash
cd group-buy-market/docs/dev-ops
docker-compose -f docker-compose-environment.yml up -d
```

### 2. 数据库

```bash
mysql -h 127.0.0.1 -P 13306 -u root -p123456 < group-buy-market/docs/dev-ops/mysql/sql/2-29-group_buy_market.sql
mysql -h 127.0.0.1 -P 13306 -u root -p123456 group_buy_market < group-buy-market/docs/dev-ops/mysql/sql/patch-multi-books.sql
mysql -h 127.0.0.1 -P 13306 -u root -p123456 < s-pay-mall-ddd-market/docs/dev-ops/mysql/sql/s-pay-mall-ddd-market.sql
```

### 3. 启动服务

```bash
# 营销 8091
cd group-buy-market && mvn -DskipTests spring-boot:run -pl group-buy-market-app

# 商城 8070
cd s-pay-mall-ddd-market && mvn -DskipTests spring-boot:run -pl s-pay-mall-ddd-app
```

### 4. 监控栈（可选）

```bash
cd group-buy-market/docs/dev-ops
docker compose -f docker-compose-grafana.yml up -d
# Prometheus http://127.0.0.1:19090  Grafana http://127.0.0.1:4000 (admin/admin123)
```

### 5. 访问

- 书目列表：<http://127.0.0.1:8070/catalog.html>
- 拼团详情：<http://127.0.0.1:8070/index.html?goodsId=9890001>

---

## 关键接口

| 服务 | 方法 | 路径 |
|------|------|------|
| 营销 | POST | `/api/v1/gbm/trade/lock_market_pay_order` |
| 营销 | POST | `/api/v1/gbm/trade/settlement_market_pay_order` |
| 商城 | POST | `/api/v1/order/create_pay_order` |
| 商城 | POST | `/api/v1/alipay/alipay_notify_url` |

---

## 模块说明

- [group-buy-market/README.md](group-buy-market/README.md) — 营销中台
- [s-pay-mall-ddd-market/README.md](s-pay-mall-ddd-market/README.md) — 支付商城
- [HANDOFF.md](HANDOFF.md) — 接手与踩坑清单

---

## License

[MIT](LICENSE)

---

## 参考与致谢

业务模型与 DDD 分层参考了开源社区拼团教程实现；本人在此基础上完成 Vue 商城、个人中心、多商品扩展、公网部署、**可观测与故障演练**等工程化改造。感谢 [bugstack.cn](https://bugstack.cn/) 系列资料提供的初始思路；业务代码包名为 `com.shuxiang.groupbuy`，规则树/DCC 等能力依赖 `cn.bugstack.wrench` 组件库。
