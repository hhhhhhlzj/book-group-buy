# 书香拼团 · Vue 商城

独立端口前端（方案 A）。后端仍为 `:8070` 商城 + `:8091` 营销。

## 启动

```powershell
# 先启动中间件、8070、8091（见仓库根 README）

cd f:\实习\s-pay-mall-vs-group-buy\s-pay-mall-ddd-market\mall-web

# Windows：若 npm 报「禁止运行脚本」，用 npm.cmd 代替 npm（见下方说明）
npm.cmd install
npm.cmd run dev
```

浏览器：`http://127.0.0.1:5173/login` → 登录 → `/mall/`

### Windows PowerShell 执行策略

若出现 `无法加载文件 ... npm.ps1 ... 禁止运行脚本`，任选其一：

```powershell
# 推荐：直接调用 cmd 包装器，不改系统策略
npm.cmd install
npm.cmd run dev
npm.cmd run build
npm.cmd run preview
```

或单次绕过（仅当前窗口）：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
npm install
```

## 生产预览

```powershell
npm.cmd run build
npm.cmd run preview
```

浏览器：`http://127.0.0.1:8080/login`

## 路由

| 路径 | 说明 |
|------|------|
| `/login` | 登录 |
| `/mall/` | 商城首页（分类 + 商品列表） |
| `/mall/goods/:goodsId` | 拼团详情 |
| `/mall/orders` | 订单列表 |

顶栏 **智能客服** 跳转 `http://{host}:8092/group-buy-chat.html`（需单独启动 `ai-agent-group-buy-cs`）；详情页会带上当前 `goodsId`。

开发态 API 经 Vite 代理：`/api`→8070，`/gbm`→8091，`/images`→8070。

商品 manifest 唯一维护点：`src/data/books.js`（含 `categoryId`）。
