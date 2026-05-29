# 拼团监控栈（方案 A · 切片 2）

## 前置条件

1. 中间件：`docker-compose -f docker-compose-environment.yml up -d`
2. 两个 Spring Boot 已启动且 health 正常：
   - 营销 `http://127.0.0.1:8091/actuator/health`
   - 商城 `http://127.0.0.1:8070/actuator/health`

## 启动监控

```powershell
cd f:\实习\s-pay-mall-vs-group-buy\group-buy-market\docs\dev-ops
docker compose -f docker-compose-grafana.yml up -d
```

| 组件 | 地址 | 说明 |
|------|------|------|
| Prometheus | http://127.0.0.1:19090 | 宿主机 **9090** 常被 xxl-job 占用，映射到 **19090** |
| Grafana | http://127.0.0.1:4000 | 默认 `admin` / `admin123` |
| Dashboard | 拼团监控 → **拼团业务监控** | 自动 provisioning |

## 切片 2 验收

### 1. Prometheus Targets 双 UP

浏览器打开：http://127.0.0.1:19090/targets  

应看到 `group-buy-apps` 下 **2 个 target** 均为 **UP**（8091、8070）。

PowerShell：

```powershell
(Invoke-WebRequest "http://127.0.0.1:19090/api/v1/targets" -UseBasicParsing).Content | ConvertFrom-Json |
  Select-Object -ExpandProperty data | Select-Object -ExpandProperty activeTargets |
  Select-Object scrapeUrl, health, labels
```

### 2. 指标存在

```powershell
Invoke-WebRequest "http://127.0.0.1:19090/api/v1/query?query=up{job=`"group-buy-apps`"}" -UseBasicParsing |
  Select-Object -ExpandProperty Content
```

### 3. 打一次锁单，Grafana 出现 QPS 曲线

```powershell
$body = @{
  userId = "user001"
  teamId = $null
  activityId = 100123
  goodsId = "9890001"
  source = "s01"
  channel = "c01"
  notifyMQ = $true
  outTradeNo = (Get-Random -Maximum 999999999999)
} | ConvertTo-Json

Invoke-WebRequest -Method POST `
  -Uri "http://127.0.0.1:8091/api/v1/gbm/trade/lock_market_pay_order" `
  -ContentType "application/json" `
  -Body $body -UseBasicParsing
```

打开 Grafana → **拼团业务监控** → 面板「锁单 QPS」应出现 success 曲线（约 15s 内刷新）。

### 4. 停止

```powershell
docker compose -f docker-compose-grafana.yml down
```

## 故障排查

| 现象 | 处理 |
|------|------|
| Target DOWN | 确认 8070/8091 已启动；Windows 需 Docker Desktop，`host.docker.internal` 可用 |
| Grafana 无 Dashboard | 检查 `grafana/provisioning/dashboards` 与 `grafana/dashboards/group-buy-business.json` |
| Grafana 一直 Restarting | 勿挂载旧 `grafana.ini`（其中 MySQL 3306 错误）；compose 已改为默认 sqlite3 |
| 19090 端口冲突 | 修改 `docker-compose-grafana.yml` 左侧端口映射 |
| 锁单 QPS 始终为 0 | 先 POST 锁单；Prometheus `scrape_interval` 15s，面板用 1m rate |

## 可选：MySQL 存 Grafana

本 compose 默认 **SQLite**（`grafana-data` volume）。若要用 MySQL：

1. 执行 `mysql/sql/grafana.sql`（库在 `13306`，密码 `123456`）
2. 在 compose 中改 `GF_DATABASE_*` 并去掉 `GF_DATABASE_TYPE=sqlite`
