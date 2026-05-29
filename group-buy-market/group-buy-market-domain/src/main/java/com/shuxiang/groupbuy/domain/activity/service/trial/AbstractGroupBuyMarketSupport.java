package com.shuxiang.groupbuy.domain.activity.service.trial;

import com.shuxiang.groupbuy.domain.activity.adapter.repository.IActivityRepository;
import com.shuxiang.groupbuy.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import com.shuxiang.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @description 抽象的拼团营销支撑类
 * @create 2024-12-14 13:42
 */
public abstract class AbstractGroupBuyMarketSupport<MarketProductEntity, DynamicContext, TrialBalanceEntity> extends AbstractMultiThreadStrategyRouter<com.shuxiang.groupbuy.domain.activity.model.entity.MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, com.shuxiang.groupbuy.domain.activity.model.entity.TrialBalanceEntity> {

    protected long timeout = 5000;

    @Resource
    protected IActivityRepository repository;

    @Override
    protected void multiThread(com.shuxiang.groupbuy.domain.activity.model.entity.MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 缺省的方法
    }

}
