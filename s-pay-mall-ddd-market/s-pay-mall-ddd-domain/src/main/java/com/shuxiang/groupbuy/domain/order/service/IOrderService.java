package com.shuxiang.groupbuy.domain.order.service;

import com.shuxiang.groupbuy.domain.order.model.entity.OrderEntity;
import com.shuxiang.groupbuy.domain.order.model.entity.PayOrderEntity;
import com.shuxiang.groupbuy.domain.order.model.entity.ShopCartEntity;
import com.alipay.api.AlipayApiException;

import java.util.Date;
import java.util.List;

public interface IOrderService {

    PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception;

    void changeOrderPaySuccess(String orderId, Date orderTime);

    List<String> queryNoPayNotifyOrder();

    List<String> queryTimeoutCloseOrderList();

    boolean changeOrderClose(String orderId);

    void changeOrderMarketSettlement(List<String> outTradeNoList);

    List<OrderEntity> queryUserOrderList(String userId, Long lastId, Integer pageSize);

    /**
     * 营销退单
     */
    boolean refundMarketOrder(String userId, String orderId);

    /**
     * 接收拼团退单消息
     */
    boolean refundPayOrder(String userId, String orderId) throws AlipayApiException;

}
