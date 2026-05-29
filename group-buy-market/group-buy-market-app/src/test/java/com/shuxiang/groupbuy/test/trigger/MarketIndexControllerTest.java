package com.shuxiang.groupbuy.test.trigger;

import com.shuxiang.groupbuy.api.dto.GoodsListRequestDTO;
import com.shuxiang.groupbuy.api.dto.GoodsListResponseDTO;
import com.shuxiang.groupbuy.api.dto.GoodsMarketRequestDTO;
import com.shuxiang.groupbuy.api.dto.GoodsMarketResponseDTO;
import com.shuxiang.groupbuy.api.response.Response;
import com.shuxiang.groupbuy.trigger.http.MarketIndexController;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * @description 营销首页服务
 * @create 2025-02-02 16:05
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class MarketIndexControllerTest {

    @Resource
    private MarketIndexController marketIndexController;

    @Test
    public void test_queryGroupBuyMarketConfig() {
        GoodsMarketRequestDTO requestDTO = new GoodsMarketRequestDTO();
        requestDTO.setSource("s01");
        requestDTO.setChannel("c01");
        requestDTO.setUserId("user001");
        requestDTO.setGoodsId("9890001");

        Response<GoodsMarketResponseDTO> response = marketIndexController.queryGroupBuyMarketConfig(requestDTO);

        log.info("请求参数:{}", JSON.toJSONString(requestDTO));
        log.info("应答结果:{}", JSON.toJSONString(response));
    }

    @Test
    public void test_queryGoodsList() {
        GoodsListRequestDTO requestDTO = new GoodsListRequestDTO();
        requestDTO.setSource("s01");
        requestDTO.setChannel("c01");

        Response<GoodsListResponseDTO> response = marketIndexController.queryGoodsList(requestDTO);

        log.info("请求参数:{}", JSON.toJSONString(requestDTO));
        log.info("应答结果:{}", JSON.toJSONString(response));
    }

}
