# 拼团项目故障演练记录（方案 A · 切片 4）

> 环境：本机 `8070` 商城 + `8091` 营销 + docker 中间件 + Prometheus `19090` + Grafana `4000`  
> 指标：`mall_alipay_notify_total`、`gbm_notify_task_pending`

---

## 场景 1：重复支付回调

### 注入方式

对已 **PAY_SUCCESS** 的订单，连续 2 次调用商城「主动查单回调」接口（模拟支付宝重复 notify）：

```cmd
f:\实习\s-pay-mall-vs-group-buy\scripts\chaos\duplicate-pay-notify.cmd <你的orderId>
```

或：

```powershell
$oid = "你的商户订单号"
Invoke-WebRequest -Method POST "http://127.0.0.1:8070/api/v1/alipay/active_pay_notify?outTradeNo=$oid" -UseBasicParsing
Invoke-WebRequest -Method POST "http://127.0.0.1:8070/api/v1/alipay/active_pay_notify?outTradeNo=$oid" -UseBasicParsing
```

### 预期现象

| 观测点 | 预期 |
|--------|------|
| 应用日志 | 第 2 次出现 `支付回调幂等跳过` |
| Prometheus | `mall_alipay_notify_total{result="duplicate"}` +1 |
| 订单表 `pay_order` | `status` 保持 `PAY_SUCCESS`，不重复结算 |
| 营销结算 | 不重复调用 `settlementMarketPayOrder` |

### 代码锚点

- 幂等：`s-pay-mall-ddd-domain/.../OrderService.java` → `changeOrderPaySuccess`
- 指标：`s-pay-mall-ddd-app/.../PayNotifyMetricsAspect.java`

### 实际记录（请你压测后填写）

| 项 | 记录 |
|----|------|
| 时间 | |
| orderId | |
| 第 1 次 HTTP 响应 | |
| 第 2 次 HTTP 响应 | |
| duplicate counter | |
| Grafana 截图 | `docs/chaos/grafana-duplicate-callback.png`（可选） |

---

## 场景 2：RabbitMQ 不可用 → notify_task 积压 → 补偿恢复

### 注入方式

```bash
docker stop rabbitmq
```

在此状态下完成「拼团成团」或触发会写 `notify_task` 且需发 MQ/HTTP 回调的流程。

### 预期现象

| 观测点 | 预期 |
|--------|------|
| `notify_task` 表 | `notify_status in (0,2)` 条数上升 |
| Grafana | `gbm_notify_task_pending` 曲线抬高 |
| 应用 | 发布/消费异常日志，业务库事务仍可能提交 |

### 恢复方式

```bash
docker start rabbitmq
```

手动补偿（dev 专用，需 **8091** 使用 `dev` profile）：

```powershell
Invoke-WebRequest -Method POST "http://127.0.0.1:8091/api/v1/dev/chaos/run-notify-job" -UseBasicParsing
```

或脚本：`scripts/chaos/mq-chaos-recover.cmd`

### SQL 验证

```sql
-- 库：group_buy_market @ 13306
SELECT notify_status, COUNT(*) AS cnt
FROM notify_task
GROUP BY notify_status;

SELECT team_id, notify_status, notify_count, update_time
FROM notify_task
WHERE notify_status IN (0, 2)
ORDER BY update_time DESC
LIMIT 10;
```

### 代码锚点

- 积压 Gauge：`group-buy-market-app/.../NotifyTaskMetricsScheduler.java`
- 手动补偿：`group-buy-market-trigger/.../ChaosDevController.java`
- 补偿逻辑：`group-buy-market-domain/.../TradeTaskService.java`

### 实际记录（请你演练后填写）

| 项 | 记录 |
|----|------|
| 时间 | |
| stop MQ 时刻 | |
| pending 峰值 | |
| start MQ + run-notify-job 后 pending | |
| successCount（接口返回） | |
| Grafana 截图 | `docs/chaos/grafana-mq-recovery.png`（可选） |

---

## 结论（面试 30 秒版）

1. **重复回调**：订单状态幂等 + `duplicate` 指标可观测，避免重复结算。  
2. **MQ 故障**：本地消息表 `notify_task` 积压可见，MQ 恢复后 Job/手动接口补偿，最终一致。  
3. **排障路径**：Grafana 指标 → 日志 `trace-id` → 表 `notify_task` / `pay_order`。

---

## 复现前检查

- [ ] 中间件：`docker-compose -f docker-compose-environment.yml up -d`（MySQL / Redis / **RabbitMQ**）
- [ ] **8070** 商城、**8091** 营销已启动（8091 必须带 **`dev`** profile，否则没有补偿接口）
- [ ] 营销用 **`install -am` 编译** 后再启动（见下方启动命令）
- [ ] Prometheus **19090** + Grafana **4000** 已起（可选但建议开，方便填表）

```powershell
# 营销（务必 -am）
cd f:\实习\s-pay-mall-vs-group-buy\group-buy-market
mvn -DskipTests install -pl group-buy-market-app -am
mvn -DskipTests spring-boot:run -pl group-buy-market-app -am

# 商城（另一个窗口）
cd f:\实习\s-pay-mall-vs-group-buy\s-pay-mall-ddd-market
mvn -DskipTests spring-boot:run -pl s-pay-mall-ddd-app -am

# 监控（第三个窗口，可选）
cd f:\实习\s-pay-mall-vs-group-buy\group-buy-market\docs\dev-ops
docker compose -f docker-compose-grafana.yml up -d
```

---

## 手把手操作（填「实际记录」用）

### 场景 1：重复支付回调

#### 第 0 步：找一个「已支付」的订单号 `orderId`

`orderId` = 表 `pay_order.order_id`（商户订单号，一般 12 位数字），**不是**表里的自增 `id`。

**方式 A（推荐）— 浏览器走一笔支付**

1. 打开 <http://127.0.0.1:8070/catalog.html> → 选一本书 → 登录/下单 → 支付宝沙箱付完（或你们环境里的「确认支付」）。
2. 打开订单列表 <http://127.0.0.1:8070/order-list.html>，复制一条 **已支付** 订单号。

**方式 B — 查库**

```powershell
# 需本机有 mysql 客户端；密码按你 application-dev.yml
mysql -h127.0.0.1 -P13306 -uroot -p123456 -e "SELECT order_id, status, pay_time FROM s_pay_mall.pay_order WHERE status='PAY_SUCCESS' ORDER BY id DESC LIMIT 5;"
```

记下其中一列 `order_id`，例如 `928263928388`。

#### 第 1 步：记 Prometheus 当前 duplicate 值（可选）

浏览器打开（把查询里的时间去掉或保留）：

```text
http://127.0.0.1:19090/graph?g0.expr=mall_alipay_notify_total%7Bresult%3D%22duplicate%22%7D&g0.tab=0
```

或在 PowerShell：

```powershell
(Invoke-WebRequest "http://127.0.0.1:19090/api/v1/query?query=mall_alipay_notify_total{result=`"duplicate`"}" -UseBasicParsing).Content
```

记下 `value` 里当前的数字（没有 metric 时可能是空，记为 0）。

#### 第 2 步：连续打两次「主动查单回调」

```cmd
f:\实习\s-pay-mall-vs-group-buy\scripts\chaos\duplicate-pay-notify.cmd 928263928388
```

把 `928263928388` 换成你的 `orderId`。

**说明**：该接口会查支付宝沙箱；订单在支付宝侧也必须是 **TRADE_SUCCESS**，否则会返回「交易状态非成功」。若订单只在库里是 `PAY_SUCCESS` 但沙箱查不到，可改用下面「纯幂等」方式：

```powershell
# 不经过支付宝查询，直接调领域层（仅本地验证幂等日志时用）
# 正常验收仍建议 active_pay_notify
$oid = "你的orderId"
Invoke-WebRequest -Method POST "http://127.0.0.1:8070/api/v1/alipay/active_pay_notify?outTradeNo=$oid" -UseBasicParsing
Invoke-WebRequest -Method POST "http://127.0.0.1:8070/api/v1/alipay/active_pay_notify?outTradeNo=$oid" -UseBasicParsing
```

#### 第 3 步：看日志与指标

| 看什么 | 怎么做 |
|--------|--------|
| 日志 | 商城 8070 控制台搜：`支付回调幂等跳过`（第 2 次应出现） |
| Prometheus | 再查一次 duplicate，应 **≥ 上次 +1**（已支付订单两次调用通常 **+2**，也算幂等生效） |
| Grafana | 打开 <http://127.0.0.1:4000> → 拼团监控 → 面板「支付回调（5m increase）」→ `duplicate` 有曲线 |

#### 第 4 步：填 chaos-log「场景 1 实际记录」

| 项 | 示例 |
|----|------|
| 时间 | 2026-05-29 10:30 |
| orderId | 928263928388 |
| 第 1 次 HTTP 响应 | `{"code":"0000","data":"交易成功，订单状态已更新"}` 或已是 duplicate 路径 |
| 第 2 次 HTTP 响应 | 同上或仍 0000，但日志有幂等跳过 |
| duplicate counter | 0 → 2（贴 Prometheus JSON 或数字） |
| Grafana 截图 | 另存为 `docs/chaos/grafana-duplicate-callback.png` |

---

### 场景 2：MQ 停服 → notify_task 积压 → 恢复

#### 第 0 步：确认有「待补偿」任务

```sql
-- 库 group_buy_market @ 13306
SELECT notify_status, COUNT(*) cnt FROM notify_task GROUP BY notify_status;
SELECT team_id, notify_status, notify_count, update_time
FROM notify_task WHERE notify_status IN (0, 2) ORDER BY update_time DESC LIMIT 5;
```

- 若 **已有** `notify_status=0` 或 `2` 的行 → 可直接做第 1 步。
- 若 **没有** → 先走一遍「拼团成团」（2 人团：两个账号各锁单+支付），或暂时跳过成团，用下面「制造一条待补偿」：

```sql
-- 仅本机演练：插入一条假任务（team_id 换成你库里真实存在的团）
-- INSERT INTO notify_task (...)  -- 一般不推荐，优先用真实成团
```

更简单：**先不停 MQ**，浏览器完成一次 2 人成团，确认 `notify_task` 里出现过 `status=0`，再重复实验。

#### 第 1 步：看积压基线

Grafana 面板 **「notify_task 积压」**，或：

```powershell
(Invoke-WebRequest "http://127.0.0.1:19090/api/v1/query?query=gbm_notify_task_pending" -UseBasicParsing).Content
```

记下数值，例如 `pending=1`。

#### 第 2 步：停 RabbitMQ

```powershell
docker stop rabbitmq
```

（只停 MQ，**不要**停 MySQL/Redis。）

#### 第 3 步：触发补偿失败 / 积压上升

```powershell
Invoke-WebRequest -Method POST "http://127.0.0.1:8091/api/v1/dev/chaos/run-notify-job" -UseBasicParsing
```

- 8091 日志里可能有 MQ 连接失败。
- 再查 SQL / Grafana：`notify_status in (0,2)` 条数或 `gbm_notify_task_pending` **维持或升高**。

可再执行 1～2 次 `run-notify-job`，观察 `notify_count` 增加、`status=2`（重试）是否变多。

#### 第 4 步：恢复 MQ 并补偿

```powershell
docker start rabbitmq
# 等约 10～20 秒让连接恢复
Invoke-WebRequest -Method POST "http://127.0.0.1:8091/api/v1/dev/chaos/run-notify-job" -UseBasicParsing
```

返回 JSON 示例：

```json
{"code":"0000","data":{"waitCount":1,"successCount":1,"errorCount":0,"retryCount":0}}
```

记下 `successCount`、`waitCount`。

#### 第 5 步：验证回落

```sql
SELECT notify_status, COUNT(*) cnt FROM notify_task GROUP BY notify_status;
```

Grafana 上 `gbm_notify_task_pending` 应在补偿成功后 **下降**（刷新间隔约 30s）。

#### 第 6 步：填 chaos-log「场景 2 实际记录」

| 项 | 示例 |
|----|------|
| 时间 | 2026-05-29 11:00 |
| stop MQ 时刻 | 11:01 |
| pending 峰值 | 2 |
| start MQ + run-notify-job 后 pending | 0 |
| successCount | 1 |
| Grafana 截图 | `docs/chaos/grafana-mq-recovery.png` |

---

## 常见问题

| 问题 | 处理 |
|------|------|
| `duplicate-pay-notify` 无反应 | 8070 没起；或 orderId 错；或支付宝沙箱查单非 TRADE_SUCCESS |
| 没有 `mall_alipay_notify_total` | 8070 未重新编译切片 4；Prometheus 未抓 8070 |
| `run-notify-job` 404 | 8091 未用 `dev` profile |
| `countPendingNotifyTasks` 编译失败 | 用 `mvn install -pl group-buy-market-app -am` |
| pending 一直 0 | 库里没有 `notify_status in (0,2)`，先成团或先跑一次失败补偿 |
