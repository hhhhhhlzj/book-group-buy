# 拼团项目 · 面试讲稿（方案 A）

> 建议总时长 **3 分钟** 讲架构 + 任选 **1～2 个** 亮点深挖。代码路径均相对于 `s-pay-mall-vs-group-buy/`。

---

## 一、3 分钟整体架构（背诵版）

**S（背景）**  
这是一个双服务拼团交易系统：商城负责下单和支付宝回调，营销负责锁单、成团和退款，中间用 REST 同步调用，异步用 RabbitMQ，跨服务一致性靠营销侧 `notify_task` 本地消息表 + 定时/手动补偿。

**T（任务）**  
我负责把「能跑 demo」提升到「能讲清上线后怎么排障」：接 Prometheus/Grafana、压测找瓶颈、做重复回调和 MQ 故障演练。

**A（行动）**  
1. 两服务接 Micrometer，Prometheus 抓 8070/8091，Grafana 看锁单 QPS、耗时、支付回调 duplicate、notify 积压。  
2. HTTP 入口加 `X-Trace-Id`，商城调营销 OkHttp 透传，日志用同一 trace-id 串联。  
3. 50 并发压锁单，发现 Tomcat 仅 20 线程导致 P99 顶到 30s，用独立 `dev,perf` profile 调大线程池对比。  
4. 支付回调加幂等和 `mall.alipay.notify` 指标；MQ 停服看 `gbm_notify_task_pending`，恢复后 dev 接口触发补偿。

**R（结果）**  
主链路可演示（公网 catalog）；有压测报告、故障记录和 Dashboard 截图支撑；重复回调不会重复结算，MQ 抖动后任务可补偿。

---

## 二、亮点 STAR

### 亮点 1：全链路可观测

| | |
|--|--|
| **S** | 原项目监控 compose 有，但应用未暴露业务指标，出问题只能翻日志。 |
| **T** | 锁单慢、回调重复时能在 1 分钟内定位到服务与接口。 |
| **A** | Actuator/prometheus；`LockOrderMetricsAspect`；`TraceIdFilter` + Retrofit 透传；Grafana 业务看板。 |
| **R** | Prometheus 可见 `gbm_lock_order_*`；Grafana 可看 QPS/耗时；日志可按 trace-id 搜。 |

**代码**：`group-buy-market-app/.../LockOrderMetricsAspect.java`、`TraceIdFilter.java`、`docs/dev-ops/MONITORING.md`

**反追问**  
- Q：为什么不用 SkyWalking？  
  A：21h 内先用 Micrometer + Prometheus 落地；trace 用轻量 traceId，深度链路可后续接 OTel。  
- Q：公网为何不开 Grafana？  
  A：2C4G 内存紧，公网只演示业务；监控本机 + 文档截图。

---

### 亮点 2：指标驱动优化（Tomcat）

| | |
|--|--|
| **S** | 压测锁单 P99 约 30s，像接口很慢。 |
| **T** | 区分业务慢还是容器排队。 |
| **A** | k6/并行脚本 50 并发；Grafana 看 tomcat 线程忙；对比 `application-dev` vs `application-perf.yml`。 |
| **R** | 根因之一是 `max-threads=20` 排队；`dev,perf` 后 **QPS +50%、均值 −21%**；P99 仍受 30s 压测超时封顶，尾延迟可继续查 DB/连接池。 |

**代码**：`application-dev.yml`（基线）、`application-perf.yml`（优化）、`docs/perf/perf-report.md`

**反追问**  
- Q：为什么先调 Tomcat 不调 Redis？  
  A：指标和压测都指向排队超时，先改最明显约束；Redis 可在下一轮用相同方法验证。  
- Q：优化后 QPS 多少？  
  A：以你本机 `results-after-tomcat.txt` 为准，面试报真实数字。

---

### 亮点 3：故障可观测（重复回调 + MQ）

| | |
|--|--|
| **S** | 支付渠道可能重复 notify；MQ 短暂不可用会导致回调发不出去。 |
| **T** | 要幂等且能看见 duplicate 与积压，恢复后能补偿。 |
| **A** | `OrderService` 已支付则跳过；`PayNotifyMetricsAspect`；`gbm_notify_task_pending`；`ChaosDevController.run-notify-job`。 |
| **R** | 重复请求 duplicate+1、订单状态不变；停 MQ 后 pending 升、恢复后补偿下降。 |

**代码**：`OrderService.changeOrderPaySuccess`、`PayNotifyMetricsAspect.java`、`NotifyTaskMetricsScheduler.java`、`docs/chaos/chaos-log.md`

**反追问**  
- Q：为什么不用 Seata？  
  A：跨服务最终一致用本地消息表 + MQ + 补偿更简单，和 2C 资源匹配。  
- Q：幂等键是什么？  
  A：订单维度：已 `PAY_SUCCESS`/`DEAL_DONE` 不再推进；营销结算还有 `outTradeNo`、notify_task `uuid`。

---

## 三、简历 bullet 对照（可直接粘贴）

1. **拼团锁单**：责任链 + Redis 抢占 + DB 乐观锁；50 并发压测，`dev,perf` 调 Tomcat 后 QPS 3.2→4.9（+50%）、均值 3.6s→2.8s；Prometheus/Grafana 看锁单耗时。  
2. **支付结算**：本地消息表 + RabbitMQ + 定时/手动补偿；`traceId` 串联商城→营销；支付回调幂等与 `duplicate` 指标。  
3. **可观测运维**：Grafana 业务看板（锁单 QPS/耗时、回调、notify 积压）；故障演练重复回调与 MQ 停服（`docs/chaos/chaos-log.md`）。

---

## 四、演示顺序（5 分钟现场）

1. 打开公网 `catalog.html` → 进一本书 → 试算/锁单。  
2. Grafana：锁单 QPS、notify 积压面板。  
3. `duplicate-pay-notify.cmd` 或口述 duplicate 曲线。  
4. 指 README「项目演进」表 + perf/chaos 文档路径。
