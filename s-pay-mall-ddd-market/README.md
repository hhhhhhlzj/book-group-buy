# s-pay-mall-ddd-market

基于 DDD 架构的支付商城服务（与 group-buy-market 配套，共同实现拼团下单 + 支付 + 营销结算闭环）。

## 模块结构

- `s-pay-mall-ddd-api` 对外接口契约（DTO / Service Interface）
- `s-pay-mall-ddd-app` 启动模块（Spring Boot Application、配置）
- `s-pay-mall-ddd-domain` 领域层（聚合 / 实体 / 领域服务）
- `s-pay-mall-ddd-infrastructure` 基础设施（DAO / Redis / Gateway）
- `s-pay-mall-ddd-trigger` 适配器层（HTTP Controller / MQ Listener / Job）
- `s-pay-mall-ddd-types` 通用类型（枚举 / 异常 / SDK）

## 启动

1. 启动中间件：MySQL / Redis / RabbitMQ（参考 `docs/dev-ops/`）
2. 导入 SQL：`docs/dev-ops/mysql/sql/s-pay-mall-ddd-market.sql`
3. 修改 `s-pay-mall-ddd-app/src/main/resources/application-dev.yml` 中的连接信息和支付 / 微信凭证
4. 运行 `s-pay-mall-ddd-app` 模块（主类 `com.shuxiang.groupbuy.Application`），默认端口 `8070`
5. 同时启动配套的 `group-buy-market` 服务（端口 `8091`）

## 商城静态页与多商品

静态资源维护两份并保持内容一致：

- Nginx 部署目录：[`docs/dev-ops/nginx/html/`](docs/dev-ops/nginx/html/)
- 内置静态（随 jar）：[`s-pay-mall-ddd-app/src/main/resources/static/`](s-pay-mall-ddd-app/src/main/resources/static/)

### 页面入口

- **全部书目**：[`catalog.html`](docs/dev-ops/nginx/html/catalog.html) — 网格浏览，点击进入 `index.html?goodsId=...`。
- **商品详情**：[`index.html`](docs/dev-ops/nginx/html/index.html) — 支持 URL 参数 `goodsId`；书名与轮播等展示由 [`js/books.js`](docs/dev-ops/nginx/html/js/books.js) 中的 `BOOK_PAGES` 驱动；拼团数据仍请求营销服务 `query_group_buy_market_config`。

### 营销库多 SKU（必做）

在已导入 `group-buy-market` 基础库的前提下，执行增量脚本：

[`group-buy-market/docs/dev-ops/mysql/sql/patch-multi-books.sql`](../group-buy-market/docs/dev-ops/mysql/sql/patch-multi-books.sql)

为 `9890002`～`9890005` 增加 `sku`、`group_buy_activity`（独立 `activity_id`，避免不同书拼团队伍串单）、`sc_sku_activity` 映射。执行前请备份数据库。

### 冒烟建议

1. 登录后打开 `catalog.html`，逐一点击 5 本书进入详情，确认标题与接口返回的 `activityId` 随 `goodsId` 变化（新书活动为 `100124`～`100127`）。
2. 在某一本书详情页打开「智能客服」，确认新标签 URL 中的 `goodsId` 与当前书一致。
3. 任选一书走「试算 → 单独购买 / 开团」锁单路径，确认订单中的 `productId` 与所选 `goodsId` 一致。
