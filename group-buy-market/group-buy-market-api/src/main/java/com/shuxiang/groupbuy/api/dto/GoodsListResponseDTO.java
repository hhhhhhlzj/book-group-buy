package com.shuxiang.groupbuy.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * @description 商品列表（catalog）应答
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoodsListResponseDTO {

    private List<GoodsItem> goodsList;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GoodsItem {
        private String goodsId;
        private String goodsName;
        private Long activityId;
        private BigDecimal originalPrice;
        private BigDecimal deductionPrice;
        private BigDecimal payPrice;
        private Integer allTeamUserCount;
    }

}
