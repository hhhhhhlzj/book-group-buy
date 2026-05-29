package com.shuxiang.groupbuy.trigger.http;

import com.shuxiang.groupbuy.domain.auth.service.ILoginService;
import com.shuxiang.groupbuy.types.sdk.weixin.MessageTextEntity;
import com.shuxiang.groupbuy.types.sdk.weixin.SignatureUtil;
import com.shuxiang.groupbuy.types.sdk.weixin.XmlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 微信服务对接，对接地址：<a href="http://your-domain.example.com/api/v1/weixin/portal/receive">/api/v1/weixin/portal/receive</a>
 * <p>
 * http://your-domain.example.com/api/v1/weixin/portal/receive/
 */
@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/weixin/portal/")
public class WeixinPortalController {

    @Value("${weixin.config.originalid}")
    private String originalid;
    @Value("${weixin.config.token}")
    private String token;
    /** 可选：扫码后公众号回复里的兜底登录页（须与接口配置同一台 8070，如 http://公网IP:8080） */
    @Value("${weixin.config.login-page-url:}")
    private String loginPageUrl;

    @Resource
    private ILoginService loginService;

    @GetMapping(value = "receive", produces = "text/plain;charset=utf-8")
    public String validate(@RequestParam(value = "signature", required = false) String signature,
                           @RequestParam(value = "timestamp", required = false) String timestamp,
                           @RequestParam(value = "nonce", required = false) String nonce,
                           @RequestParam(value = "echostr", required = false) String echostr) {
        try {
            log.info("微信公众号验签信息开始 [{}, {}, {}, {}]", signature, timestamp, nonce, echostr);
            if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
                throw new IllegalArgumentException("请求参数非法，请核实!");
            }
            boolean check = SignatureUtil.check(token, signature, timestamp, nonce);
            log.info("微信公众号验签信息完成 check：{}", check);
            if (!check) {
                return null;
            }
            return echostr;
        } catch (Exception e) {
            log.error("微信公众号验签信息失败 [{}, {}, {}, {}]", signature, timestamp, nonce, echostr, e);
            return null;
        }
    }

    /**
     * 微信推送消息仅为 POST body（XML）；URL 查询参数仅有 signature / timestamp / nonce（明文模式下），
     * 用户 openid 在 XML 节点 {@code FromUserName} 中，不能作为必选 RequestParam，否则绑定失败返回 400，
     * 用户端会看到「该公众号提供的服务出现故障」。
     */
    @PostMapping(value = "receive", produces = "application/xml; charset=UTF-8")
    public String post(@RequestBody String requestBody,
                       @RequestParam(value = "signature", required = false) String signature,
                       @RequestParam(value = "timestamp", required = false) String timestamp,
                       @RequestParam(value = "nonce", required = false) String nonce,
                       @RequestParam(name = "encrypt_type", required = false) String encType,
                       @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        String openid = "";
        try {
            if (StringUtils.isBlank(requestBody)) {
                log.warn("微信公众号 POST body 为空");
                return "";
            }
            if (StringUtils.isNotBlank(encType)) {
                log.warn("检测到微信 encrypt_type={}，当前服务端仅支持明文模式；请到公众平台测试号关闭「消息加解密」或改为明文。", encType);
                return "";
            }
            // 明文模式：与 GET 验签相同（加密模式下需解密与 msg_signature 验签，此处暂未实现解密）
            if (StringUtils.isBlank(encType)
                    && StringUtils.isNoneBlank(signature, timestamp, nonce)
                    && !SignatureUtil.check(token, signature, timestamp, nonce)) {
                log.warn("微信公众号 POST 签名验证失败 signature={}", signature);
                return "";
            }

            MessageTextEntity message = XmlUtil.weixinIncomingXmlToEntity(requestBody);
            openid = message.getFromUserName();
            if (StringUtils.isBlank(openid)) {
                log.warn("微信公众号 POST 未解析出 FromUserName body={}", requestBody);
                return "";
            }

            log.info("接收微信公众号信息请求 openid={} msgType={} event={} body={}", openid,
                    message.getMsgType(), message.getEvent(), requestBody);

            // 已关注用户扫带参数二维码 → SCAN + Ticket；未关注扫码关注 → subscribe + Ticket
            if ("event".equalsIgnoreCase(message.getMsgType())
                    && StringUtils.isNotBlank(message.getTicket())) {
                if ("SCAN".equalsIgnoreCase(message.getEvent())
                        || "subscribe".equalsIgnoreCase(message.getEvent())) {
                    loginService.saveLoginState(message.getTicket(), openid);
                    String eventKey = message.getEventKey();
                    if (StringUtils.isNotBlank(eventKey)) {
                        String scene = eventKey.startsWith("qrscene_") ? eventKey.substring(8) : eventKey;
                        loginService.saveLoginStateForScene(scene, openid);
                    }
                    log.info("微信扫码登录回调成功 openid={} eventKey={} ticketLen={}",
                            openid, message.getEventKey(),
                            message.getTicket() != null ? message.getTicket().length() : 0);
                    String reply = "登录成功";
                    if (StringUtils.isNotBlank(loginPageUrl)) {
                        String bind = loginService.createLoginBindCode(openid);
                        String base = loginPageUrl.replaceAll("/+$", "");
                        reply += "。若网页未自动跳转，请点此完成登录：\n" + base + "/login?bind=" + bind;
                    }
                    return buildMessageTextEntity(openid, reply);
                }
            }

            if (StringUtils.isNotBlank(message.getContent())) {
                return buildMessageTextEntity(openid, "你好，" + message.getContent());
            }
            return "";
        } catch (Exception e) {
            log.error("接收微信公众号信息请求失败 openid={} {}", openid, requestBody, e);
            return "";
        }
    }

    private String buildMessageTextEntity(String openid, String content) {
        return XmlUtil.weixinPassiveTextXml(openid, originalid, content);
    }

}
