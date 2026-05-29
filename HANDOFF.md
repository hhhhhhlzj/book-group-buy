# 拼团项目 · Agent 交接文档

> 目标：让接手者 30 分钟内能跑起项目、知道改哪里、不踩雷。

---

**下一阶段（商城页面 + 功能完善）请优先阅读：**  
👉 **[HANDOFF-NEXT-AGENT.md](./HANDOFF-NEXT-AGENT.md)**（用户已从可观测改造转向前端/业务功能，本文 §3 部分优先级已过时）

---

## 1. 项目结构

- `s-pay-mall-vs-group-buy/group-buy-market`：营销中台（端口 **8091**），DDD 分层；
  拼团活动配置、试算、锁单、结算、退款；MQ + 本地消息表保证最终一致性。
- `s-pay-mall-vs-group-buy/s-pay-mall-ddd-market`：支付商城（端口 **8070**），
  下单 / 支付宝沙箱 / 商城静态页（含 `index.html` / `catalog.html` / `order-list.html`）。
- `ai-agent/primProj/ai-agent-group-buy-cs`：拼团智能客服（端口 **8092**，Spring AI + Google ADK）。
- `ai-agent/primProj/ai-agent-scaffold-lite`：通用脚手架，YAML 装配 ChatModel / MCP / Workflow。

## 2. 当前进度（已完成）

- 拼团核心链路：试算（规则树）、锁单（责任链 + Redis 抢占 + 乐观锁）、结算（本地消息表 + HTTP/MQ 双模回调）、退款（策略模式 + RabbitMQ）。
- 动态配置：Redis Pub/Sub + 注解 + 反射的 DCC。
- 商城静态页：双份维护
  - Nginx 部署：`s-pay-mall-ddd-market/docs/dev-ops/nginx/html/`
  - 内置 jar：`s-pay-mall-ddd-market/s-pay-mall-ddd-app/src/main/resources/static/`
- **多商品**：`catalog.html` + `js/books.js`，`index.html?goodsId=...` 动态渲染；
  营销库增量脚本：`group-buy-market/docs/dev-ops/mysql/sql/patch-multi-books.sql`
  （新书 `9890002~9890005` 各自独立 `activity_id` 100124~100127，避免组队串单）。
- 智能客服：`ai-agent-group-buy-cs` 通过 RestClient + Tool 调营销接口；商城页带参跳客服。

## 3. 还没做 / 可能要做（建议优先级从高到低）

1. **多商品冒烟**：在 MySQL 中执行 `patch-multi-books.sql` 后，逐一打开 5 本书走「试算 → 锁单 → 支付」一遍，确认 `activityId` 随 `goodsId` 切换、组队列表互不污染。
2. **营销侧商品列表接口**（可选）：当前 `catalog.html` 用前端静态 manifest，若要展示「真实拼团价 / 在团人数」，需新增接口（建议 `POST /api/v1/gbm/index/query_goods_list`），由 catalog 拉取。
3. **监控（切片 1~2）**：两服务已接 Actuator + `gbm_lock_order_*` 指标；`docker-compose-grafana.yml` 启动 Prometheus（**19090**）+ Grafana（**4000**），Dashboard 见 `docs/dev-ops/MONITORING.md`。
4. **故障演练（切片 4）**：`docs/chaos/chaos-log.md`；脚本 `scripts/chaos/`；幂等+`mall.alipay.notify`；`gbm_notify_task_pending`；8091 `POST /api/v1/dev/chaos/run-notify-job`（仅 dev）。
4. **退款 / 结算回归测试**：单测在 `group-buy-market-app/src/test/`，覆盖了主链路；新书加入后建议用 `100124` 跑一次 `ITradeLockOrderServiceTest` / `ITradeReverseStockServiceTest`。
5. **简历演示链接**：`http://111.228.26.5:8070/` 一定带 `http://`（之前漏 `//`）。

## 4. 启动顺序（本地）

```bash
cd s-pay-mall-vs-group-buy/group-buy-market/docs/dev-ops
docker-compose -f docker-compose-environment.yml up -d   # mysql / redis / rabbitmq

mysql -h127.0.0.1 -P13306 -uroot -p123456 < mysql/sql/2-29-group_buy_market.sql
mysql -h127.0.0.1 -P13306 -uroot -p123456 < mysql/sql/patch-multi-books.sql
mysql -h127.0.0.1 -P13306 -uroot -p123456 < ../../../s-pay-mall-ddd-market/docs/dev-ops/mysql/sql/s-pay-mall-ddd-market.sql
```

随后用 IDE 跑两个 `Application`：

- `com.shuxiang.groupbuy.Application`（group-buy-market，8091）
- `com.shuxiang.groupbuy.Application`（s-pay-mall-ddd，8070）

可选：`ai-agent-group-buy-cs`（8092），需配 LLM key（见其 `application-local.yml`）。

## 5. 改动「禁区」与坑

- **静态页双份必须一起改**：Nginx html/ 与 Spring static/ 不同步会出现「本地正常，云上 404」。
- **`group_buy_activity` 不要复用 `100123`**：多书共用一个活动会让组队列表混在一起（`MarketIndexController` 按 `activityId` 拉队伍）。
- **`AppConfig.goodsId` 仅作 fallback**：详情页应优先用 URL `?goodsId=`；客服 `openCustomerService(goodsId)` 也要带当前书。
- **支付宝沙箱**：`application-dev.yml` 里 `app_id` / 密钥 / `notify_url` 在公网才能回调，本地只能用「确认支付」弹窗手动跳过。
- **本地消息表**：定时任务 `GroupBuyNotifyJob` / `TimeoutRefundJob` 会扫，调试时可以缩短间隔，否则等回调要好几分钟。

## 6. 关键文件速查

| 用途 | 路径 |
|------|------|
| 拼团试算入口 | `group-buy-market-trigger/.../MarketIndexController.java` |
| 锁单链 | `group-buy-market-domain/.../trade/service/lock/...` |
| 商城详情页 | `s-pay-mall-ddd-market/docs/dev-ops/nginx/html/index.html` |
| 商城浏览页 | `s-pay-mall-ddd-market/docs/dev-ops/nginx/html/catalog.html` |
| 商品 manifest | `s-pay-mall-ddd-market/docs/dev-ops/nginx/html/js/books.js` |
| 增量 SQL | `group-buy-market/docs/dev-ops/mysql/sql/patch-multi-books.sql` |
| 中间件 compose | `group-buy-market/docs/dev-ops/docker-compose-environment.yml` |
| 监控 compose | `group-buy-market/docs/dev-ops/docker-compose-grafana.yml` |

## 7. 还不确定的边界

- 商城库 `s-pay-mall-ddd-market.sql` 中的商品记录只在订单维度有 `goods_id` 字符串，**没有独立商品表**；新书是否需要在商城侧建表，取决于业务需求，**当前未做**。
- 智能客服里的 LLM provider / key 没有提交到仓库，接手者需要自己配（见各 `application-*.yml` 占位）。
- 本地图片仅 3 张占位，5 本书都共用，**未替换真实封面**。

## 8. 沟通约定（给下一个 Agent）

- 改静态页：**Nginx 包 + Spring static/ 必须双写**，PR 里要能看到两份 diff。
- 加新接口：先在 `group-buy-market-api` 写 DTO + Service 接口，再写 Controller / Domain，保持 DDD 分层。
- 涉及活动/商品的 SQL：放到独立 `patch-*.sql`，不要直接改 `2-29-group_buy_market.sql` 的历史 dump。

---

## 附：能不能同时跑 MySQL + Redis + RabbitMQ + Prometheus

**结论：能，但要看机器内存和同时跑几个 Spring Boot 应用。**

### 容器自身的常态占用（参考值）

| 服务 | 内存（空载～轻载） | 端口（compose 默认） |
|------|---------------------|---------------------|
| MySQL 8 | 400–800 MB | `13306` |
| Redis 6 | 50–150 MB | `16379` |
| RabbitMQ 3.12 | 300–500 MB | `5672 / 15672` |
| Prometheus | 150–300 MB | `9090` |
| Grafana（一起开时） | 150–250 MB | `4000` |

仅 4 个中间件 ≈ **1～1.8 GB**；加抓取频率与保留时长后 Prometheus 会涨。

### 加上 Java 应用

| 应用 | JVM 默认占用 | 端口 |
|------|---------------|------|
| `group-buy-market` | 400–700 MB | 8091 |
| `s-pay-mall-ddd-app` | 400–700 MB | 8070 |
| `ai-agent-group-buy-cs` | 500–800 MB | 8092 |

三个 Spring Boot 一起 ≈ **1.5–2 GB**。

### 综合建议

| 场景 | 评估 |
|------|------|
| 本机 8 GB 内存 | 中间件全开 + 1 个 Spring Boot 没问题；3 个全开会比较紧 |
| 本机 16 GB 内存 | **舒服**，全部并行无压力 |
| 云服 2C2G | 跑不动监控栈，建议只开 MySQL + Redis + RabbitMQ |
| 云服 2C4G（当前 8070 环境） | **够用但紧**：可加监控，Prometheus 保留时长建议 ≤ 7 天 |
| 云服 2C8G / 4C8G | 推荐，全部 + Grafana + 应用都能跑 |

### 想让它「能同时跑」的几个小动作

1. **限内存**：每个服务加 `mem_limit`，例如 `mysql: mem_limit: 512m`、`prometheus: mem_limit: 256m`。
2. **JVM 限堆**：Spring Boot 启动加 `-Xms256m -Xmx512m`，三个加起来约 1.5 GB 可控。
3. **Prometheus 抓取频率**：默认 15s 即可，不要降到 5s；可加 `--storage.tsdb.retention.time=7d` 省磁盘。
4. **端口不冲突**：现有映射 `13306 / 16379 / 5672 / 9090 / 4000` 已避开常用端口，沿用即可。
5. **网络**：现有 compose 用 `my-network: bridge`，监控栈如单独 compose 起，需 `external: true` 或合并 project，否则 Prometheus 抓不到中间件指标。

> 实操建议：本地先 `docker-compose-environment.yml`（MySQL + Redis + MQ）调代码；监控栈按需起，不必 24 小时常开。
