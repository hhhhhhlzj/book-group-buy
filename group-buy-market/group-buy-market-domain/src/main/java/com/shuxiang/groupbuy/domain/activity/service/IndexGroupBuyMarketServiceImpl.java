package com.shuxiang.groupbuy.domain.activity.service;

import com.shuxiang.groupbuy.domain.activity.adapter.repository.IActivityRepository;
import com.shuxiang.groupbuy.domain.activity.model.entity.GoodsListItemEntity;
import com.shuxiang.groupbuy.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.shuxiang.groupbuy.domain.activity.model.entity.MarketProductEntity;
import com.shuxiang.groupbuy.domain.activity.model.entity.TrialBalanceEntity;
import com.shuxiang.groupbuy.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.shuxiang.groupbuy.domain.activity.model.valobj.SkuVO;
import com.shuxiang.groupbuy.domain.activity.model.valobj.TeamStatisticVO;
import com.shuxiang.groupbuy.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @description 首页营销服务
 * @create 2024-12-14 14:33
 */
@Service
@Slf4j
public class IndexGroupBuyMarketServiceImpl implements IIndexGroupBuyMarketService {

    @Resource
    private DefaultActivityStrategyFactory defaultActivityStrategyFactory;
    @Resource
    private IActivityRepository repository;

    @Override
    public TrialBalanceEntity indexMarketTrial(MarketProductEntity marketProductEntity) throws Exception {
        // 获取执行策略
        StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> strategyHandler = defaultActivityStrategyFactory.strategyHandler();
        // 受理试算操作
        return strategyHandler.apply(marketProductEntity, new DefaultActivityStrategyFactory.DynamicContext());
    }

    @Override
    public List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailList(Long activityId, String userId, Integer ownerCount, Integer randomCount) {
        List<UserGroupBuyOrderDetailEntity> unionAllList = new ArrayList<>();

        // 查询个人拼团数据
        if (0 != ownerCount) {
            List<UserGroupBuyOrderDetailEntity> ownerList = repository.queryInProgressUserGroupBuyOrderDetailListByOwner(activityId, userId, ownerCount);
            if (null != ownerList && !ownerList.isEmpty()){
                unionAllList.addAll(ownerList);
            }
        }

        // 查询其他非个人拼团
        if (0 != randomCount) {
            List<UserGroupBuyOrderDetailEntity> randomList = repository.queryInProgressUserGroupBuyOrderDetailListByRandom(activityId, userId, randomCount);
            if (null != randomList && !randomList.isEmpty()){
                unionAllList.addAll(randomList);
            }
        }

        return unionAllList;
    }

    @Override
    public TeamStatisticVO queryTeamStatisticByActivityId(Long activityId) {
        return repository.queryTeamStatisticByActivityId(activityId);
    }

    @Override
    public List<GoodsListItemEntity> queryGoodsList(String source, String channel, String userId) {
        List<SkuVO> skuList = repository.querySkuListBySourceChannel(source, channel);
        if (null == skuList || skuList.isEmpty()) {
            return Collections.emptyList();
        }

        List<GoodsListItemEntity> result = new ArrayList<>();
        for (SkuVO sku : skuList) {
            try {
                TrialBalanceEntity trialBalanceEntity = indexMarketTrial(MarketProductEntity.builder()
                        .userId(userId)
                        .source(source)
                        .channel(channel)
                        .goodsId(sku.getGoodsId())
                        .build());

                GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = trialBalanceEntity.getGroupBuyActivityDiscountVO();
                if (null == groupBuyActivityDiscountVO || null == groupBuyActivityDiscountVO.getActivityId()) {
                    log.warn("queryGoodsList skip goodsId={} missing activity", sku.getGoodsId());
                    continue;
                }

                Long activityId = groupBuyActivityDiscountVO.getActivityId();
                TeamStatisticVO teamStatisticVO = queryTeamStatisticByActivityId(activityId);
                Integer allTeamUserCount = teamStatisticVO != null ? teamStatisticVO.getAllTeamUserCount() : 0;

                result.add(GoodsListItemEntity.builder()
                        .goodsId(trialBalanceEntity.getGoodsId())
                        .goodsName(trialBalanceEntity.getGoodsName())
                        .activityId(activityId)
                        .originalPrice(trialBalanceEntity.getOriginalPrice())
                        .deductionPrice(trialBalanceEntity.getDeductionPrice())
                        .payPrice(trialBalanceEntity.getPayPrice())
                        .allTeamUserCount(allTeamUserCount != null ? allTeamUserCount : 0)
                        .build());
            } catch (Exception e) {
                log.warn("queryGoodsList skip goodsId={}", sku.getGoodsId(), e);
            }
        }
        return result;
    }

}
