package com.shuxiang.groupbuy.api;

import com.shuxiang.groupbuy.api.dto.GoodsListRequestDTO;
import com.shuxiang.groupbuy.api.dto.GoodsListResponseDTO;
import com.shuxiang.groupbuy.api.dto.GoodsMarketRequestDTO;
import com.shuxiang.groupbuy.api.dto.GoodsMarketResponseDTO;
import com.shuxiang.groupbuy.api.response.Response;

/**
 * @description 营销首页服务接口
 * @create 2025-02-02 16:02
 */
public interface IMarketIndexService {

    /**
     * 查询拼团营销配置
     *
     * @param goodsMarketRequestDTO 营销商品信息
     * @return 营销配置信息
     */
    Response<GoodsMarketResponseDTO> queryGroupBuyMarketConfig(GoodsMarketRequestDTO goodsMarketRequestDTO);

    /**
     * 查询渠道商品列表（catalog 动态数据）
     *
     * @param goodsListRequestDTO 渠道与可选 userId
     * @return 商品列表（拼团价、在团人数）
     */
    Response<GoodsListResponseDTO> queryGoodsList(GoodsListRequestDTO goodsListRequestDTO);

}
