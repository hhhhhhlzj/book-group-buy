package com.shuxiang.groupbuy.domain.order.model.entity;

import com.shuxiang.groupbuy.domain.order.model.valobj.MarketTypeVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShopCartEntity {

    // 用户ID
    private String userId;

    // 商品ID
    private String productId;

    // 拼团组队ID，可为空，为空的时，则为用户首次创建拼团
    private String teamId;

    // 活动ID，来自于页面调用拼团试算后，获得的活动ID信息
    private Long activityId;

    // 营销类型，无营销，拼团营销
    private MarketTypeVO marketTypeVO;

    // 收货地址（下单快照）
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String receiverRegion;
    private String receiverDetail;

}
