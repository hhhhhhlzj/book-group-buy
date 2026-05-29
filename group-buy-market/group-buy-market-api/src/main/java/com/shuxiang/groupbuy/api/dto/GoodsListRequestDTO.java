package com.shuxiang.groupbuy.api.dto;

import lombok.Data;

/**
 * @description 商品列表（catalog）请求
 */
@Data
public class GoodsListRequestDTO {

    /** 渠道来源，如 s01 */
    private String source;
    /** 渠道，如 c01 */
    private String channel;
    /** 用户 ID；空则服务端使用 catalog_guest */
    private String userId;

}
