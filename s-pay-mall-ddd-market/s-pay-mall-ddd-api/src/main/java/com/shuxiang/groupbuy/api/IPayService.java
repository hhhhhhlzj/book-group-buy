package com.shuxiang.groupbuy.api;

import com.shuxiang.groupbuy.api.dto.CreatePayRequestDTO;
import com.shuxiang.groupbuy.api.dto.NotifyRequestDTO;
import com.shuxiang.groupbuy.api.dto.QueryOrderListRequestDTO;
import com.shuxiang.groupbuy.api.dto.QueryOrderListResponseDTO;
import com.shuxiang.groupbuy.api.dto.RefundOrderRequestDTO;
import com.shuxiang.groupbuy.api.dto.RefundOrderResponseDTO;
import com.shuxiang.groupbuy.api.response.Response;

public interface IPayService {

    Response<String> createPayOrder(CreatePayRequestDTO createPayRequestDTO);

    /**
     * 拼团结算回调
     *
     * @param requestDTO 请求对象
     * @return 返参，success 成功
     */
    String groupBuyNotify(NotifyRequestDTO requestDTO);

    /**
     * 查询用户订单列表
     *
     * @param requestDTO 请求对象
     * @return 订单列表
     */
    Response<QueryOrderListResponseDTO> queryUserOrderList(QueryOrderListRequestDTO requestDTO);

    /**
     * 用户退单
     *
     * @param requestDTO 请求对象
     * @return 退单结果
     */
    Response<RefundOrderResponseDTO> refundOrder(RefundOrderRequestDTO requestDTO);

    Response<String> activePayNotify(String outTradeNo);
}
