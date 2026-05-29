package com.shuxiang.groupbuy.domain.auth.service;

public interface ILoginService {

    String createQrCodeTicket() throws Exception;

    String createQrCodeTicket(String sceneStr) throws Exception;

    String checkLogin(String ticket);

    String checkLogin(String ticket, String sceneStr);

    void saveLoginState(String ticket, String openid);

    void saveLoginStateForScene(String sceneStr, String openid);

    String createLoginBindCode(String openid);

    String exchangeLoginBindCode(String code);

}
