# 微信公众平台扫码登录 — 本次排错与实现说明

本文记录在本项目中打通「测试公众号 + Web 扫码登录」（`/api/v1/weixin/portal/receive`、`/api/v1/login/*`）时遇到的问题、原因与处理方式，便于后续复盘或写入简历/交接文档。

---

## 1. 前置条件一览

| 项目 | 说明 |
|------|------|
| 运行环境 | Spring Boot 2.7.x，`s-pay-mall-ddd-app`，端口一般为 **8070** |
| Java | 服务端曾使用 **JDK 17**（与下文 XStream 问题直接相关）；本地开发也可用 JDK 8/17（注意 IDE / Maven 与 Lombok 一致） |
| 微信公众平台 | **测试号**：[微信公众平台测试账号](https://mp.weixin.qq.com/debug/cgi-bin/sandbox?t=sandbox/login) |
| 外网可达 | 接口配置 URL 必须公网可访问（如云服务器 **公网 IP + 端口**）；安全组放行 **TCP 8070**（或通过 Nginx 反向代理） |
| 数据库 / 中间件 | MySQL、Redis、RabbitMQ 等与项目 `application-prod.yml`/`application-dev.yml` 一致 |

关键配置项（示意，勿将真实密钥提交公开仓库）：

- **`weixin.config.token`**：与测试号「接口配置」里的 Token **完全一致**。
- **`weixin.config.app-id` / `app-secret`**：测试号页的 **AppID（wx…）** 与 **AppSecret**。
- **`weixin.config.originalid`**：必须为测试号页的 **微信号（一般以 `gh_` 开头）**。  
  **不能填写 AppID（wx…）**。被动回复 XML 里的 `FromUserName` 需使用该值；填错时，手机端常见提示为「该公众号提供的服务出现故障」。

---

## 2. 回调与请求形态（易混点）

微信对「接口配置 URL」会做 **GET 验签**（`signature`、`timestamp`、`nonce`、`echostr`），与业务上的 **POST 收消息** 是两条路径。

- **GET**：仅用于配置校验；可用 Postman / 浏览器按官方规则拼签名自测。
- **POST**：正文为 **XML**；URL 查询参数通常只有 **`signature`、`timestamp`、`nonce`（明文模式）**。  
  **用户 `openid` 在 XML 节点 `FromUserName` 中**，不会作为 `openid` 查询参数传递。

若后端将 `openid` 写成 **必填的 `@RequestParam("openid")`**，Spring 绑定失败会返回 **400**，用户侧往往表现为「服务出现故障」。

---

## 3. 本次遇到的主要问题与处理

### 3.1 POST 未按微信真实参数设计（`openid` RequestParam）

**现象**：GET 验签在 Postman 中成功，扫码后仍报服务故障。

**原因**：微信 POST 不包含 `openid` 查询参数；必选 `RequestParam` 导致请求失败。

**处理**：移除对 `openid` 的强制入参，从 XML 解析 `FromUserName`；并对 **SCAN / subscribe + Ticket** 两种事件做登录态写入（首次关注常为 `subscribe`，老用户扫参扫为 `SCAN`）。

---

### 3.2 `originalid` 误填为 AppID（`wx…`）

**现象**：配置看似正确，扫码后仍不稳定或报服务故障。

**原因**：被动回复里 `FromUserName` 应为公众号 **微信号 `gh_…`**，不能使用 **AppID（`wx…`）**。

**处理**：在 `application-*.yml` 中将 **`weixin.config.originalid`** 改为测试号页的 **`gh_`** 原始微信号。

---

### 3.3 轮询接口 `openidToken` 长期为 `null`（GET 中 `ticket` 未编码）

**现象**：日志中 `check_login_scene` 频繁打印 `openidToken:null`，但 `ticket` 与 `sceneStr` 看似有值。

**原因**：微信返回的 `ticket` 常含 **`+`**。若前端把 `ticket` **直接拼进 GET 查询字符串** 且未 `encodeURIComponent`，`+` 会被解析为 **空格**，与 Guava Cache 中保存的原始 `ticket` **字符串不一致**，校验永远失败。

**处理**：

- 前端：`weixin_qrcode_ticket_scene`、`check_login_scene`、以及 `showqrcode` 的图片地址，均对 **`ticket`、`sceneStr` 使用 `encodeURIComponent`**。
- 后端：`checkLogin(ticket, sceneStr)` 增加 **空格与 `+` 的兼容**，且用缓存中的规范 **`cacheTicket`** 去取 `openid`（与回调写入的 key 一致）。

---

### 3.4 JDK 17 + XStream：`InaccessibleObjectException` / `TreeMapConverter`

**现象**：日志在 `WeixinPortalController.post` 调用 `XmlUtil.xmlToBean` 时出现：

`Unable to make field private final java.util.Comparator java.util.TreeMap.comparator accessible: module java.base does not "opens java.util"`

**原因**：旧版 **XStream** 初始化时会反射访问 JDK 内部 `TreeMap`，在 **JDK 17 模块封装** 下失败，导致 **微信 XML 根本解析不了**，`saveLoginState` 不会执行，表现为轮询一直 **`openidToken:null`**（与 3.3 并发时易误判）。

**处理**：对 **微信入站/出站 XML** 不再依赖 XStream：

- 使用已有依赖 **dom4j** 解析入站 XML → `MessageTextEntity`（`weixinIncomingXmlToEntity`）。
- 被动文本回复改为 **手写拼接 XML**（`weixinPassiveTextXml`）。

（项目内若仍有其他路径调用 `xmlToBean`/`beanToXml`，在 JDK 17 下同样可能踩坑，需单独评估。）

---

### 3.5 根 POM 混用 `spring-boot-starter-amqp` 版本

**现象**：运行或关闭进程时出现 `ClassNotFoundException` / `NoClassDefFoundError`（如 `ConsumerDispatcher$2`、`SocketFrameHandler$1`）。

**原因**：曾在父 POM 的 `dependencyManagement` 中把 **`spring-boot-starter-amqp` 写成 3.2.0**，与 **Spring Boot 2.7.12** 主版本不一致，易导致 **AMQP 客户端与 Spring 集成类版本错配**。

**处理**：删掉该条 **错误的固定版本**，由 **`spring-boot-starter-parent` BOM** 统一管理（与 Boot 2.7 对齐）。

---

### 3.6 日志被识别为二进制，`grep` 无输出

**现象**：`grep` 提示 `Binary file ... matches`。

**原因**：日志中夹杂非文本字符。

**处理**：使用 `grep -a` 强制按文本检索，或轮换/清空日志文件便于阅读。

---

### 3.7 模板消息与「登录是否成功」的关系

`template_id` 未配置或为占位时，发送模板消息接口可能失败。**登录态写入（`ticket → openid`）应优先成功**；建议在业务层对模板发送 **单独 try-catch**，避免因模板失败误判整次回调失败（具体以当前代码为准）。

---

## 4. 部署与自检清单

1. **`mvn` 必须在多模块根目录执行**（含 `s-pay-mall-ddd-app` 的那层 `pom.xml`），例如：  
   `mvn -pl s-pay-mall-ddd-app -am clean package -DskipTests`
2. **上传新 jar** 后重启；配置在 **打包进 jar 的 `application-*.yml`**，未换包则配置不生效。
3. 安全组放行 **8070**（及实际对外端口）。
4. 浏览器访问登录页建议 **强制刷新**（Ctrl+F5），避免沿用旧版 `login.html`。
5. 服务器侧快速确认：  
   `ss -tlnp | grep 8070`  
   `grep -a "接收微信公众号信息" /path/to/s-pay-mall.log | tail`

---

## 5. 小结（可写进简历的一句话）

在 **Spring Boot 2.7 + JDK 17** 环境下完成微信测试号扫码登录联调：修正 **POST 参数与 XML 解析链路**、**`gh_`/`wx` 配置错误**、**GET 传参下 `ticket` 的 URL 编码**，并将 **XStream 替换为 dom4j + 手写回复 XML** 以解决 **JPMS 反射限制**；同时修正父 POM **RabbitMQ Starter 与 Boot 主版本混用**导致的类加载异常。

---

*文档对应工程路径：`s-pay-mall-ddd-market`；主要涉及 `WeixinPortalController`、`XmlUtil`、`WeixinLoginService`、`LoginController`、前端 `login.html` 及根 `pom.xml` 中与 AMQP 相关的依赖管理。*
