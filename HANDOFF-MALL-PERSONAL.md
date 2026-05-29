# 交接文档 · Vue 商城 + 个人中心（下一 Agent）

> **用户最新意图**：在现有 Vue 商城上 **增加个人中心**（收货地址、收藏、浏览记录等）。  
> **当前动作**：vibecoding **第一步已完成**（问题定义）；**请下一 Agent 从第二步（列方案）开始**，勿直接大写代码。  
> 工作区：`f:\实习\`；仓库：`f:\实习\s-pay-mall-vs-group-buy\`

---

## 1. 先读这些（按顺序）

| 优先级 | 文件 | 用途 |
|--------|------|------|
| 1 | [`拼团项目-个人中心-问题定义.md`](../拼团项目-个人中心-问题定义.md) | **本阶段锚点**（第一步） |
| 2 | [`拼团项目-Vue商城-问题定义.md`](../拼团项目-Vue商城-问题定义.md) v2.2 | Vue 商城已锁定决策 |
| 3 | [`拼团项目-Vue商城-系统骨架.md`](../拼团项目-Vue商城-系统骨架.md) | 路由/API/端口 |
| 4 | [`mall-web/README.md`](./s-pay-mall-ddd-market/mall-web/README.md) | 启动与 Windows npm.cmd |
| 5 | [`HANDOFF-NEXT-AGENT.md`](./HANDOFF-NEXT-AGENT.md) | 8070/8091/方案 A 通用坑 |
| 6 | [`拼团项目-问题定义.md`](../拼团项目-问题定义.md) | 项目总目标（面试/公网） |

---

## 2. 已完成（不要误删/重复做）

### 2.1 Vue 商城 MVP（方案 A）

- 工程：`s-pay-mall-ddd-market/mall-web/`（Vue 3 + Vite + Vue Router）
- 路由：
  - `/login` — 微信扫码 + 无痕登录（FingerprintJS）
  - `/mall/` — 分类 Tab（全部/框架/中间件/JVM）+ 商品列表
  - `/mall/goods/:goodsId` — 详情、试算、开团/参团/单独购买
  - `/mall/orders` — 订单列表、退单
- 数据：`src/data/books.js`（含 `categoryId`）+ `8091 query_goods_list`
- 端口：dev **5173**，preview **8080**；API **8070** + **8091**
- 鉴权：cookie `loginToken`；路由 `requiresAuth`
- 构建：`npm.cmd run build` 已通过

### 2.2 营销后端（8091）

- 新增 `POST /api/v1/gbm/index/query_goods_list`（catalog 动态价/在团人数）
- 编译：`mvn -DskipTests install -pl group-buy-market-app -am`

### 2.3 方案 A（后端/运维）

- traceId、Prometheus、支付幂等、chaos 文档等 — 见 HANDOFF-NEXT-AGENT §4  
- **除非用户要求，勿再深挖监控/压测**

### 2.4 已废弃方向

- 静态页 HTML **双份手改**（catalog 等）→ 已并入 Vue，**勿再维护**
- 静态 catalog 动态化作为终态 → 已被 Vue 商城取代

---

## 3. 下一 Agent 要做什么

### 3.1 用户新需求（个人中心）

- 个人信息/「我的」页
- **收货地址**（增删改、默认地址）
- **收藏**
- **浏览记录**

### 3.2 vibecoding 进度

| 步 | 个人中心阶段 | 状态 |
|----|--------------|------|
| 第一步 问题定义 | [`拼团项目-个人中心-问题定义.md`](../拼团项目-个人中心-问题定义.md) v1.2 | ✅ |
| 第二步 方案 | **方案 A 已推荐**（见 §3.4） | ✅ |
| 第三步 骨架 md | `拼团项目-个人中心-系统骨架.md` | ✅ |
| 第四步 编码 | 三表 + API + Vue 子路由 + 下单带地址 | ✅（待跑 SQL + 运行时验收） |
| 第五步 异常/边界 | 与用户一起做 | ⏳ |

### 3.3 已锁定（用户确认 U1–U4）

| ID | 决策 |
|----|------|
| U1 | **MySQL + 8070 API** |
| U2 | **下单要带收货地址**（改 `create_pay_order` + `pay_order`） |
| U3 | 收藏、浏览各 **5 条** |
| U4 | **多子路由**（`/mall/profile/address` 等） |

### 3.4 第二步方案 A（推荐，已选）

**三表 + 统一 UserCenter API + 下单快照**

| 组件 | 做法 |
|------|------|
| DB | `user_address`、`user_favorite`、`user_browse_history`；`pay_order` 加 `address_id` + 快照列 |
| API 前缀 | `/api/v1/user/`（地址/收藏/浏览 CRUD） |
| 下单 | `CreatePayRequestDTO.addressId` → 校验归属 → 写订单快照 |
| Vue | `/mall/profile` 布局 + 子路由 address / favorites / history；详情页收藏按钮 + 下单前选地址 |

**不选**：localStorage 主存储（违 U1）；单页 Tab（违 U4）；收藏/浏览仅前端（违 U1）。

**下一 Agent**：先执行 `patch-user-center.sql`，再起 8070/5173 做运行时验收；与用户一起做第五步异常清单。

### 3.5 第二步前曾待对齐（已关闭）

~~U1–U4~~ → 见 §3.3。

---

## 4. 启动命令（Windows）

```powershell
# 中间件
cd f:\实习\s-pay-mall-vs-group-buy\group-buy-market\docs\dev-ops
docker compose -f docker-compose-environment.yml up -d

# 营销 8091
cd f:\实习\s-pay-mall-vs-group-buy\group-buy-market
mvn -DskipTests install -pl group-buy-market-app -am
mvn -DskipTests spring-boot:run -pl group-buy-market-app -am

# 商城 8070（另一窗口）
cd f:\实习\s-pay-mall-vs-group-buy\s-pay-mall-ddd-market
mvn -DskipTests spring-boot:run -pl s-pay-mall-ddd-app -am

# Vue 前端（另一窗口）— 注意用 npm.cmd
cd f:\实习\s-pay-mall-vs-group-buy\s-pay-mall-ddd-market\mall-web
npm.cmd install
npm.cmd run dev
# → http://127.0.0.1:5173/login
```

---

## 5. 关键路径速查

| 资源 | 路径 |
|------|------|
| Vue 入口 | `mall-web/src/main.js` |
| 路由 | `mall-web/src/router/index.js` |
| 顶栏 | `mall-web/src/components/AppLayout.vue` |
| 商品 manifest | `mall-web/src/data/books.js` |
| 8070 登录 API | `LoginController` `/api/v1/login/*` |
| 8070 订单 API | `AliPayController` `/api/v1/alipay/*` |
| 8091 列表 | `MarketIndexController` `query_goods_list` |
| 公网演示 | API `http://111.228.26.5:8070/`；Vue 需 **8080**（若未放通则仅本机） |

---

## 6. 头号风险（下一 Agent 必读）

1. **个人中心 + 三套数据 + 可能新表** → 先 MVP 再加深，别一次做满
2. **PowerShell** → `npm.cmd`，不是 `npm`
3. **改 8091 后** → `mvn ... -pl group-buy-market-app -am`
4. **loginToken = 浏览器指纹** 时，收藏/地址「换设备丢失」是预期，要在方案里说明
5. **第五步**用户希望**一起做** — 编码后留异常清单给用户过一遍

---

## 7. 建议下一 Agent 第一句话

> 个人中心 MVP 已编码：[`拼团项目-个人中心-系统骨架.md`](../拼团项目-个人中心-系统骨架.md)、`patch-user-center.sql`、`UserCenterController`、`/mall/profile/*`、下单 `addressId`。请执行 SQL 后验收，并与用户做第五步异常审查。

---

_最后更新：Vue 商城 MVP 已交付；个人中心需求已录入问题定义，待下一 Agent 接第二步。_
