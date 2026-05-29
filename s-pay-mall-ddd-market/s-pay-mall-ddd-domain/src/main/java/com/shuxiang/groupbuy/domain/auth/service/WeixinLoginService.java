package com.shuxiang.groupbuy.domain.auth.service;

import com.shuxiang.groupbuy.domain.auth.adapter.port.ILoginPort;
import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
public class WeixinLoginService implements ILoginService {

    private static final String SCENE_TICKET_PREFIX = "scene_ticket:";
    private static final String BIND_PREFIX = "login_bind:";
    private static final int MAX_USER_ID_LEN = 64;

    @Resource
    private ILoginPort loginPort;
    @Resource
    private Cache<String, String> openidToken;

    @Override
    public String createQrCodeTicket() throws Exception {
        return loginPort.createQrCodeTicket();
    }

    @Override
    public String createQrCodeTicket(String sceneStr) throws Exception {
        String ticket = loginPort.createQrCodeTicket(sceneStr);
        openidToken.put(SCENE_TICKET_PREFIX + normalizeScene(sceneStr), ticket);
        return ticket;
    }

    @Override
    public String checkLogin(String ticket) {
        return resolveOpenidFromTicketKey(ticket);
    }

    @Override
    public String checkLogin(String ticket, String sceneStr) {
        String normScene = normalizeScene(sceneStr);
        String cacheTicket = openidToken.getIfPresent(SCENE_TICKET_PREFIX + normScene);
        if (StringUtils.isBlank(cacheTicket)) {
            String legacy = openidToken.getIfPresent(normScene);
            if (StringUtils.isNotBlank(legacy) && legacy.length() > MAX_USER_ID_LEN) {
                cacheTicket = legacy;
            }
        }
        if (StringUtils.isBlank(cacheTicket)) {
            return null;
        }
        if (!ticketMatches(cacheTicket, ticket)) {
            return null;
        }
        String openid = resolveOpenidFromTicketKey(cacheTicket);
        if (StringUtils.isBlank(openid)) {
            openid = openidToken.getIfPresent(normScene);
        }
        if (StringUtils.isBlank(openid) && StringUtils.isNotBlank(sceneStr)) {
            openid = openidToken.getIfPresent(normalizeScene(sceneStr));
        }
        return isLikelyOpenId(openid, cacheTicket) ? openid : null;
    }

    @Override
    public void saveLoginState(String ticket, String openid) {
        putTicketOpenid(ticket, openid);
        try {
            loginPort.sendLoginTemplate(openid);
        } catch (Exception e) {
            log.warn("登录态已写入 cache，模板消息发送失败 openid={}, err={}", openid, e.getMessage());
        }
    }

    @Override
    public void saveLoginStateForScene(String sceneStr, String openid) {
        if (!isLikelyOpenId(openid, null)) {
            return;
        }
        String norm = normalizeScene(sceneStr);
        openidToken.put(norm, openid);
        if (StringUtils.isNotBlank(sceneStr) && !sceneStr.equals(norm)) {
            openidToken.put(sceneStr, openid);
        }
        log.info("扫码登录 scene 已绑定 openid scene={}", norm);
    }

    @Override
    public String createLoginBindCode(String openid) {
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        openidToken.put(BIND_PREFIX + code, openid);
        return code;
    }

    @Override
    public String exchangeLoginBindCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        String openid = openidToken.getIfPresent(BIND_PREFIX + code.trim());
        if (StringUtils.isNotBlank(openid)) {
            openidToken.invalidate(BIND_PREFIX + code.trim());
        }
        return isLikelyOpenId(openid, null) ? openid : null;
    }

    private void putTicketOpenid(String ticket, String openid) {
        if (StringUtils.isBlank(ticket)) {
            return;
        }
        openidToken.put(ticket, openid);
        openidToken.put(ticket.replace(' ', '+'), openid);
        try {
            String decoded = URLDecoder.decode(ticket, StandardCharsets.UTF_8.name());
            if (!decoded.equals(ticket)) {
                openidToken.put(decoded, openid);
                openidToken.put(decoded.replace(' ', '+'), openid);
            }
        } catch (Exception ignored) {
            // ignore
        }
        log.info("扫码登录 ticket 已绑定 openid ticketLen={}", ticket.length());
    }

    private String resolveOpenidFromTicketKey(String ticket) {
        if (StringUtils.isBlank(ticket)) {
            return null;
        }
        String openid = openidToken.getIfPresent(ticket);
        if (StringUtils.isBlank(openid)) {
            openid = openidToken.getIfPresent(ticket.replace(' ', '+'));
        }
        return isLikelyOpenId(openid, ticket) ? openid : null;
    }

    private boolean ticketMatches(String cacheTicket, String ticket) {
        if (cacheTicket.equals(ticket)) {
            return true;
        }
        if (StringUtils.isBlank(ticket)) {
            return false;
        }
        return cacheTicket.equals(ticket.replace(' ', '+'));
    }

    private String normalizeScene(String sceneStr) {
        return sceneStr == null ? "" : sceneStr.trim().toUpperCase();
    }

    private boolean isLikelyOpenId(String value, String ticket) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        if (value.length() > MAX_USER_ID_LEN) {
            return false;
        }
        if (StringUtils.isNotBlank(ticket) && value.equals(ticket)) {
            return false;
        }
        return true;
    }

}
