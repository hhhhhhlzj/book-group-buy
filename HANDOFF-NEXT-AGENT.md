# 交接文档 · 下一阶段（商城页面 + 功能完善）

> **给下一个 Agent**：用户已从「方案 A 可观测改造」转向 **商城前端与其他业务功能**。请先读本文，再读 `HANDOFF.md`（通用踩坑）与 `README.md`（项目总览）。  
> 工作区根目录：`f:\实习\`；代码仓库：`f:\实习\s-pay-mall-vs-group-buy\`  
> 规划锚点：`f:\实习\拼团项目-问题定义.md`、`拼团项目-MVP.md`

---

## 1. 用户意图（当前阶段）

| 已完成（方案 A，后端/运维向） | 下一阶段（用户现在要做） |
|------------------------------|-------------------------|
| Micrometer + Prometheus + Grafana | **商城静态页体验**（catalog / index / order-list / login） |
| traceId、锁单/支付指标 | **其他功能**（商品列表接口、真实封面、UI 优化等） |
| 压测报告、Tomcat perf profile | 可能：**营销侧 `query_goods_list`**、订单/支付流程打磨 |
| 支付幂等、notify_task 积压指标 | 公网 `http://111.228.26.5:8070/` 演示要维持可用 |
| README/简历/面试讲稿 | chaos-log **实际记录表** 用户可能仍未填（非阻塞开发） |

**不要**在无需求时继续深挖监控/压测，除非用户明确要求。

---

## 2. 仓库与服务一览

| 组件 | 路径 | 端口 | 说明 |
|------|------|------|------|
| 营销中台 | `group-buy-market/` | **8091** | 试算、锁单、结算、退款；库 `group_buy_market` |
| 支付商城 | `s-pay-mall-ddd-market/` | **8070** | 登录、下单、支付宝、**静态商城页**；库 `s_pay_mall` |
| 智能客服（可选） | `ai-agent/.../ai-agent-group-buy-cs` | **8092** | 需 LLM key；`group-buy-chat.html` |
| 中间件 | `group-buy-market/docs/dev-ops/docker-compose-environment.yml` | 13306 / 16379 / 5672 | MySQL / Redis / RabbitMQ |
| 监控（可选） | `docker-compose-grafana.yml` | **19090** / **4000** | 本机 Prometheus/Grafana |

**公网演示**：`http://111.228.26.5:8070/`（2C4G 京东云，**不要**长时间停服做实验）。

---

## 3. 商城静态页（改 UI 必看）

### 3.1 双份同步（最高优先级坑）

任何 HTML/CSS/JS/图片改动必须 **两处一致**：

| 用途 | 路径 |
|------|------|
| Nginx / 公网部署 | `s-pay-mall-ddd-market/docs/dev-ops/nginx/html/` |
| Spring Boot 内置静态 | `s-pay-mall-ddd-market/s-pay-mall-ddd-app/src/main/resources/static/` |

只改一边 → 本机 jar 正常、云上 404，或相反。

### 3.2 页面与脚本

| 页面 | 文件 | 作用 |
|------|------|------|
| 书目列表 | `catalog.html` + `css/catalog.css` | 网格浏览，跳转 `index.html?goodsId=` |
| 商品详情/拼团 | `index.html` + `css/index.css` + `js/index.js`（内联较多） | 试算、锁单、下单、客服入口 |
| 订单列表 | `order-list.html` + `js/order-list.js` | 查单、退款 |
| 登录 | `login.html` + `js/login.js` | 微信扫码登录（测试号） |
| 测试回调 | `test-callback.html` | 主动查单 / chaos 场景 1 |
| 公共配置 | `js/common.js` | `AppConfig`：8070/8091/8092 地址；`openCustomerService` |
| 商品展示数据 | `js/books.js` | **`BOOK_PAGES` 静态 manifest**（标题、轮播、亮点） |
| 图片 | `images/book-cover.png` 等 | **5 本书共用 3 张占位图**，未换真封面 |

### 3.3 前端如何调后端

`js/common.js`：

```javascript
sPayMallUrl:       `http://${hostname}:8070`   // 商城 API
groupBuyMarketUrl: `http://${hostname}:8091`   // 营销 API
```

`index.html` 关键调用：

- 试算/活动：`POST {8091}/api/v1/gbm/index/query_group_buy_market_config`（body 含 `goodsId`、`userId`、`source`、`channel`）
- 下单：`POST {8070}/api/v1/alipay/create_pay_order`

`catalog.html`：**不调营销列表接口**，只读 `books.js` 渲染卡片。

### 3.4 多商品与活动 ID（勿串单）

- 增量 SQL：`group-buy-market/docs/dev-ops/mysql/sql/patch-multi-books.sql`
- `9890001` → `activity_id` **100123**；`9890002~9890005` → **100124~100127**（各书独立活动）
- **禁止**多书共用一个 `activity_id`，否则组队列表混在一起（`MarketIndexController` 按 `activityId` 查队伍）

`books.js` 里 `goodsId` 与营销库 `sc_sku_activity` 映射必须一致。

---

## 4. 方案 A 已落地代码（勿误删）

| 能力 | 位置 |
|------|------|
| TraceId | `*/types/.../TraceIdSupport.java`，两服务 `TraceIdFilter`；商城 `Retrofit2Config` 透传 |
| 锁单指标 | `group-buy-market-app/.../LockOrderMetricsAspect.java` |
| 支付回调指标+幂等 | `PayNotifyMetricsAspect.java`，`OrderService.changeOrderPaySuccess` |
| notify 积压 | `NotifyTaskMetricsScheduler.java`，`INotifyTaskDao.countPendingNotifyTasks` |
| dev 补偿接口 | `ChaosDevController` → `POST /api/v1/dev/chaos/run-notify-job`（**仅 dev profile**） |
| Tomcat 压测优化 | `application-perf.yml`，`spring.profiles.active=dev,perf` |

文档：

- `docs/perf/perf-report.md` — 压测：QPS 3.23→4.86，P99 仍受 30s 客户端超时限制
- `docs/chaos/chaos-log.md` — 故障演练步骤（含手把手）
- `docs/interview/talking-points.md`
- `group-buy-market/docs/dev-ops/MONITORING.md`

脚本：`scripts/load/`、`scripts/chaos/`

---

## 5. 启动命令（Windows，给下一个 Agent）

```powershell
# 中间件
cd f:\实习\s-pay-mall-vs-group-buy\group-buy-market\docs\dev-ops
docker compose -f docker-compose-environment.yml up -d

# 营销 8091（改代码后务必 -am）
cd f:\实习\s-pay-mall-vs-group-buy\group-buy-market
mvn -DskipTests install -pl group-buy-market-app -am
mvn -DskipTests spring-boot:run -pl group-buy-market-app -am

# 商城 8070（另一个窗口）
cd f:\实习\s-pay-mall-vs-group-buy\s-pay-mall-ddd-market
mvn -DskipTests spring-boot:run -pl s-pay-mall-ddd-app -am
```

压测/perf：`"-Dspring-boot.run.arguments=--spring.profiles.active=dev,perf"`

**编译坑**：只 `spring-boot:run -pl xxx-app` 不带 `-am` 时，可能用到旧的 `infrastructure` jar，报 `countPendingNotifyTasks 找不到符号`。

---

## 6. 建议的「商城 + 功能」任务清单（用户未逐条确认，作默认 backlog）

按投入产出排序，实施前可与用户确认范围：

### P0 · 页面与演示

1. **静态页双份同步检查**：改 UI 后 diff 两处 `html/` 与 `static/`
2. **catalog 真实数据**：新增营销接口 `query_goods_list`（或复用试算批量），展示拼团价/在团人数，减少纯静态 `books.js`
3. **封面图**：为 5 个 `goodsId` 各配一张图，更新 `BOOK_PAGES.slides`
4. **多商品冒烟**：5 本书各走一遍试算→锁单→支付（或沙箱），确认 `activityId` 不串

### P1 · 体验

5. **order-list**：状态文案、空态、加载失败提示
6. **index**：拼团倒计时、队伍列表刷新、错误码 `0001` 友好提示
7. **移动端适配**：catalog/index 栅格与按钮
8. **login**：登录后回跳 `?goodsId=` 保留

### P2 · 后端（若接 catalog）

9. 在 `group-buy-market-api` 定义 DTO → `MarketIndexController` → domain 查询活动+SKU
10. 商城 `catalog.html` 改为 `fetch(8091)`，**CORS** 已 `@CrossOrigin("*")` on controllers

### 明确不做（除非用户改口）

- 全量 `com.shuxiang.groupbuy` 包名迁移（已顺延）
- 引 Seata / 微服务化 / K8s
- 公网暴露 Grafana 4000（仅本机+截图）

---

## 7. 后端改动约定（DDD）

1. 新接口：**api 模块** DTO + Service 接口 → **trigger** Controller → **domain** → **infrastructure**
2. SQL：新建 `patch-*.sql`，勿改 `2-29-group_buy_market.sql` 历史 dump
3. 凭证：只改 `application-dev.yml.example`，真实 `application-dev.yml` 在 `.gitignore`

---

## 8. 关键后端入口（功能向）

| 场景 | 类/路径 |
|------|---------|
| 试算/活动配置 | `MarketIndexController.query_group_buy_market_config` |
| 锁单 | `MarketTradeController.lock_market_pay_order` |
| 下单 | `AliPayController.create_pay_order` |
| 支付回调 | `AliPayController.alipay_notify_url`、`active_pay_notify` |
| 订单列表 | `AliPayController.query_user_order_list` |
| 退款 | `AliPayController.refund_order` |
| 商城调营销 | `ProductPort` / `IGroupBuyMarketService` (Retrofit) |

---

## 9. 用户环境备忘

| 项 | 值 |
|----|-----|
| OS | Windows 10/11，PowerShell |
| 执行策略 | 直接跑 `.ps1` 常被拦；用 `scripts/load/run-lock-parallel.cmd` 或 `-ExecutionPolicy Bypass` |
| Maven | `settings.xml` 有 `mirrors` 警告，一般可忽略 |
| 本机 9090 | 可能被 xxl-job 占用；项目 Prometheus 用 **19090** |
| 简历 | `f:\实习\简历.md` — 拼团 bullet 已含可观测/压测表述 |

---

## 10. 文档索引（`f:\实习\`）

| 文件 | 用途 |
|------|------|
| `拼团项目-问题定义.md` | 总目标与验收 |
| `拼团项目-方案选择.md` | 方案 A 选型 |
| `拼团项目-系统骨架.md` | 模块与指标契约 |
| `拼团项目-MVP.md` | 切片进度勾选 |
| `s-pay-mall-vs-group-buy/README.md` | 对外 README |
| `HANDOFF.md` | 通用接手（较旧章节 3 优先级已过时，以本文为准） |

---

## 11. 给下一个 Agent 的第一句话建议

> 用户要做 **商城页面完善和其他功能**。静态资源在 `s-pay-mall-ddd-market` 的 **nginx/html 与 static 双份**；商品展示目前靠 `js/books.js`，营销真实价需新接口或扩展现有试算。方案 A 监控/压测/幂等已在后端落地，详见 `HANDOFF-NEXT-AGENT.md` §4。启动服务用 `mvn ... -pl xxx-app -am`。先问用户：优先改 catalog 动态数据、UI 美化，还是订单/支付流程？

---

_最后更新：方案 A 切片 1–5 完成；用户转向商城与功能阶段。_
