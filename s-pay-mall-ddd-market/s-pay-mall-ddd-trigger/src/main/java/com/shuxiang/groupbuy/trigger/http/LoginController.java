package com.shuxiang.groupbuy.trigger.http;

import com.shuxiang.groupbuy.api.IAuthService;
import com.shuxiang.groupbuy.api.response.Response;
import com.shuxiang.groupbuy.domain.auth.service.ILoginService;
import com.shuxiang.groupbuy.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/login/")
public class LoginController implements IAuthService {

    @Resource
    private ILoginService loginService;

    /**
     * http://your-domain.example.com/api/v1/login/weixin_qrcode_ticket
     * @return
     */
    @RequestMapping(value = "weixin_qrcode_ticket", method = RequestMethod.GET)
    @Override
    public Response<String> weixinQrCodeTicket() {
        try {
            String qrCodeTicket = loginService.createQrCodeTicket();
            log.info("生成微信扫码登录 ticket:{}", qrCodeTicket);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(qrCodeTicket)
                    .build();
        } catch (Exception e) {
            log.error("生成微信扫码登录 ticket 失败", e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * http://your-domain.example.com/api/v1/login/weixin_qrcode_ticket_scene?sceneStr=
     * @return
     */
    @RequestMapping(value = "weixin_qrcode_ticket_scene", method = RequestMethod.GET)
    @Override
    public Response<String> weixinQrCodeTicket(@RequestParam String sceneStr) {
        try {
            String qrCodeTicket = loginService.createQrCodeTicket(sceneStr);
            log.info("生成微信扫码登录 ticket:{}", qrCodeTicket);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(qrCodeTicket)
                    .build();
        } catch (Exception e) {
            log.error("生成微信扫码登录 ticket 失败", e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * http://your-domain.example.com/api/v1/login/check_login
     */
    @RequestMapping(value = "check_login", method = RequestMethod.GET)
    @Override
    public Response<String> checkLogin(@RequestParam String ticket) {
        try {
            String openidToken = loginService.checkLogin(ticket);
            log.info("扫码检测登录结果 ticket:{} openidToken:{}", ticket, openidToken);
            if (StringUtils.isNotBlank(openidToken)) {
                return Response.<String>builder()
                        .code(Constants.ResponseCode.SUCCESS.getCode())
                        .info(Constants.ResponseCode.SUCCESS.getInfo())
                        .data(openidToken)
                        .build();
            } else {
                return Response.<String>builder()
                        .code(Constants.ResponseCode.NO_LOGIN.getCode())
                        .info(Constants.ResponseCode.NO_LOGIN.getInfo())
                        .build();
            }
        } catch (Exception e) {
            log.error("扫码检测登录结果失败 ticket:{}", ticket, e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 公众号扫码后兜底：同一台 8070 上用 bind 码换 openid（解决回调与轮询不在同一进程时的跳转）
     */
    @RequestMapping(value = "exchange_bind", method = RequestMethod.GET)
    public Response<String> exchangeBind(@RequestParam String bind) {
        try {
            String openid = loginService.exchangeLoginBindCode(bind);
            if (StringUtils.isNotBlank(openid)) {
                log.info("bind 码兑换登录成功 bind={}", bind);
                return Response.<String>builder()
                        .code(Constants.ResponseCode.SUCCESS.getCode())
                        .info(Constants.ResponseCode.SUCCESS.getInfo())
                        .data(openid)
                        .build();
            }
            return Response.<String>builder()
                    .code(Constants.ResponseCode.NO_LOGIN.getCode())
                    .info(Constants.ResponseCode.NO_LOGIN.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("bind 码兑换失败 bind={}", bind, e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "check_login_scene", method = RequestMethod.GET)
    @Override
    public Response<String> checkLogin(@RequestParam String ticket, @RequestParam String sceneStr) {
        try {
            String openidToken = loginService.checkLogin(ticket, sceneStr);
            log.info("扫码检测登录结果 ticket:{} openidToken:{} sceneStr:{}", ticket, openidToken, sceneStr);
            if (StringUtils.isNotBlank(openidToken)) {
                return Response.<String>builder()
                        .code(Constants.ResponseCode.SUCCESS.getCode())
                        .info(Constants.ResponseCode.SUCCESS.getInfo())
                        .data(openidToken)
                        .build();
            } else {
                return Response.<String>builder()
                        .code(Constants.ResponseCode.NO_LOGIN.getCode())
                        .info(Constants.ResponseCode.NO_LOGIN.getInfo())
                        .build();
            }
        } catch (Exception e) {
            log.error("扫码检测登录结果失败 ticket:{}", ticket, e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}
