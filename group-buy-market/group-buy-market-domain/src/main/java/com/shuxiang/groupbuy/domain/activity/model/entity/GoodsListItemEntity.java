package com.shuxiang.groupbuy.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @description catalog 商品列表项
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoodsListItemEntity {

    private String goodsId;
    private String goodsName;
    private Long activityId;
    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;
    private Integer allTeamUserCount;

}
