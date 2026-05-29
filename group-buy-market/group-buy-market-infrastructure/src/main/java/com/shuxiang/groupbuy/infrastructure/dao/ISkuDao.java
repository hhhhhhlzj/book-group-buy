package com.shuxiang.groupbuy.infrastructure.dao;

import com.shuxiang.groupbuy.infrastructure.dao.po.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @description 商品查询
 * @create 2024-12-21 10:48
 */
@Mapper
public interface ISkuDao {

    Sku querySkuByGoodsId(String goodsId);

    List<Sku> querySkuListBySourceChannel(@Param("source") String source, @Param("channel") String channel);

}
