# group-buy-market

基于 DDD 架构的拼团营销中台。负责拼团活动配置、锁单、结算、退款，并通过 MQ 与下游商城系统对接。

## 模块结构

- `group-buy-market-api` 对外接口契约（DTO / Service Interface）
- `group-buy-market-app` 启动模块（Spring Boot Application、配置）
- `group-buy-market-domain` 领域层（聚合 / 实体 / 领域服务，含锁单 / 结算 / 退款规则链）
- `group-buy-market-infrastructure` 基础设施（DAO / Redis / Gateway）
- `group-buy-market-trigger` 适配器层（HTTP Controller / MQ Listener / Job）
- `group-buy-market-types` 通用类型（枚举 / 异常 / 通用工具）

## 启动

1. 启动中间件：MySQL / Redis / RabbitMQ（`docs/dev-ops/docker-compose-environment-aliyun.yml`）
2. 导入 SQL：`docs/dev-ops/mysql/sql/2-29-group_buy_market.sql`；若需多商品演示（与商城静态页多本书对应），再执行同目录 `patch-multi-books.sql`（执行前备份）
3. 修改 `group-buy-market-app/src/main/resources/application-dev.yml` 中的连接信息
4. 运行 `group-buy-market-app` 模块（主类 `com.shuxiang.groupbuy.Application`），默认端口 `8091`

## 对外暴露的关键接口

- `POST /api/v1/gbm/index/query_group_buy_market_config` 查询拼团营销配置
- `POST /api/v1/gbm/trade/lock_market_pay_order` 锁单
- `POST /api/v1/gbm/trade/settlement_market_pay_order` 结算
- `POST /api/v1/gbm/trade/refund_market_pay_order` 退款
