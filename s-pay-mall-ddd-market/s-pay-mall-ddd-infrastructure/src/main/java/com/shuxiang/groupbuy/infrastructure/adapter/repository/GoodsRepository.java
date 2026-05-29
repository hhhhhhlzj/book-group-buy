package com.shuxiang.groupbuy.infrastructure.adapter.repository;

import com.shuxiang.groupbuy.domain.goods.adapter.repository.IGoodsRepository;
import com.shuxiang.groupbuy.infrastructure.dao.IOrderDao;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

/**
 * @description 结算仓储服务
 * @create 2025-02-15 09:13
 */
@Repository
public class GoodsRepository implements IGoodsRepository {

    @Resource
    private IOrderDao orderDao;

    @Override
    public void changeOrderDealDone(String orderId) {
        orderDao.changeOrderDealDone(orderId);
    }

}
