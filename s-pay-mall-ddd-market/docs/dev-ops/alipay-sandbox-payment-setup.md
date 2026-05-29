# 支付宝沙箱支付配置流程（s-pay-mall-ddd-market）

本文说明如何在**支付宝开放平台沙箱**中完成密钥与应用配置，并在本项目中填写 `application-*.yml`，完成联调。**沙箱为测试环境**：使用沙箱网关与沙箱买家账号，**不会扣你真实支付宝余额**；正式收款需切换正式 `app_id`、正式网关与正式密钥（本文不涉及）。

官方说明：[沙箱环境](https://opendocs.alipay.com/common/02kkv7) | [电脑网站支付](https://opendocs.alipay.com/open/028r8t)

---

## 1. 准备工作

1. 使用浏览器登录 [支付宝开放平台](https://open.alipay.com/)。
2. 进入 **控制台 → 开发助手 → 沙箱环境**（或直接访问沙箱应用页：[沙箱应用](https://open.alipay.com/develop/sandbox/app)）。
3. 记录页面上的：
   - **APPID**（沙箱应用唯一标识）
   - **沙箱买家账号**（含登录密码、支付密码；**付款时必须使用该买家**，用日常支付宝扫码常会失败或提示二维码失效）
   - **沙箱支付宝账号**（卖家侧信息，联调时以文档为准）

---

## 2. 生成与配置密钥（RSA2）

沙箱与正式环境一样，使用 **RSA2** 签名。

1. 在沙箱应用页找到 **接口加签方式（密钥/证书）**。
2. 使用 **支付宝密钥生成工具** 或 OpenSSL 生成 **应用私钥** 与 **应用公钥**（商户侧）。
3. 将 **应用公钥** 粘贴到开放平台 **「设置应用公钥」**，保存后平台会展示 **支付宝公钥**（注意：这是支付宝侧公钥，**不是**你刚生成的应用公钥）。
4. 在本项目中需要两类密钥字符串：
   - **`merchant_private_key`**：你的 **应用私钥**（PKCS8 文本，一行；不要把文件路径写进 yml）。
   - **`alipay_public_key`**：开放平台提供的 **支付宝公钥**（用于验签异步通知等）。

若验签失败，常见原因是 **把「应用公钥」误填成 `alipay_public_key`**，请对照开放平台页面重新复制。

---

## 3. 开通沙箱产品能力

在沙箱应用 **产品列表 / 功能信息** 中，确认已添加 **电脑网站支付**（`alipay.trade.page.pay`）。  
本项目 `OrderService` 使用的就是该能力；未开通时下单或网关侧可能报错。

---

## 4. 修改项目配置（`s-pay-mall-ddd-app`）

编辑 `src/main/resources/application-dev.yml` 或 `application-prod.yml` 中的 **`alipay`** 段（与当前激活的 Spring Profile 一致）。

| 配置项 | 含义 | 沙箱典型值 / 注意 |
|--------|------|-------------------|
| `alipay.enabled` | 是否启用 | `true` |
| `alipay.app_id` | 沙箱 APPID | 沙箱应用页复制 |
| `alipay.merchant_private_key` | 应用私钥（PKCS8） | 本地生成，勿提交到公开仓库 |
| `alipay.alipay_public_key` | 支付宝公钥 | 开放平台「查看支付宝公钥」 |
| `alipay.gatewayUrl` | 网关 | **必须**为 `https://openapi-sandbox.dl.alipaydev.com/gateway.do` |
| `alipay.notify_url` | 异步通知地址 | 外网可访问的 **POST** 地址，见下文 |
| `alipay.return_url` | 同步跳转地址 | 支付完成后浏览器跳转页，需外网可访问 |

**异步通知 `notify_url`**

- 须为支付宝服务器能访问到的 **公网 URL**（本机 `127.0.0.1` 一般收不到沙箱通知）。
- 路径需与代码一致：`AliPayController` 映射为 **`/api/v1/alipay/alipay_notify_url`**（完整示例：`http://你的域名或IP:端口/api/v1/alipay/alipay_notify_url`）。
- 端口需与实际服务一致（例如 **`8070`**），且防火墙 / 安全组放行。

**同步跳转 `return_url`**

- 支付完成后用户浏览器会跳转到该地址（如商城首页或订单页）。
- 示例：`http://你的域名或IP:端口/index.html`。

配置由 `AliPayConfig` 读取 `AliPayConfigProperties`，组装为 `DefaultAlipayClient` Bean（`alipayClient`），供下单与回调验签使用。

---

## 5. 拼团相关 `notify-url`（如使用拼团）

若启用拼团营销，RabbitMQ / 渠道侧可能还配置了 **`group-buy-market.notify-url`**（或其它渠道 notify）。请保证其中的 **主机、端口** 与当前部署一致，避免拼团回调与商城端口混用（例如误写 `8080` 而实际为 `8070`）。

---

## 6. 打包、部署与验证

1. 执行打包（示例）：`mvn -pl s-pay-mall-ddd-app -am package -DskipTests`。
2. 将 jar 部署到可公网访问的环境，保证 **`notify_url` / `return_url` 指向该环境**。
3. 浏览器打开商城，登录后发起支付。
4. 出现收银台或扫码页时，使用 **沙箱买家账号** 在 **沙箱要求的客户端** 中完成支付（以开放平台当前说明为准）。
5. 观察应用日志：下单成功会打印支付表单相关日志；支付成功后应收到异步通知并更新订单（具体以 `AliPayController` 与订单服务逻辑为准）。

---

## 7. 常见问题

| 现象 | 可能原因 |
|------|----------|
| 扫码提示「二维码失效」 | 使用了非沙箱买家；或页面停留过久；电脑网站支付在手机扫码链路时效较短，可尽快扫码或用 PC 浏览器先验证 |
| 一直待支付、无回调 | `notify_url` 非公网、端口错误、防火墙拦截，或进程在支付过程中被关闭 |
| 验签失败 | `alipay_public_key` 填错（误用应用公钥）、密钥与 APPID 不匹配、编码非 UTF-8 |
| 与正式环境混淆 | 沙箱必须使用 `alipaydev.com` 网关；正式环境为 `https://openapi.alipay.com/gateway.do` 且使用正式密钥与 APPID |

---

## 8. 安全建议

- **不要将真实私钥提交到 Git**；团队内可用环境变量、私密配置中心或本地覆盖文件管理。
- 沙箱密钥与正式密钥分开保管；上线前在配置中心切换为正式参数并复核 `gatewayUrl`。

---

## 9. 相关代码位置（便于对照）

- 支付宝客户端 Bean：`s-pay-mall-ddd-app/.../config/AliPayConfig.java`
- 配置属性：`AliPayConfigProperties.java`（前缀 `alipay`）
- 创建支付单、异步通知：`s-pay-mall-ddd-trigger/.../trigger/http/AliPayController.java`
- 下单与 `biz_content`：`s-pay-mall-ddd-domain/.../order/service/OrderService.java`、`AbstractOrderService.java`

完成以上步骤并 **使用沙箱买家付款** 后，即可在沙箱环境完成端到端支付联调。
