package com.shuxiang.groupbuy.trigger.http;

import com.shuxiang.groupbuy.api.response.Response;
import com.shuxiang.groupbuy.domain.trade.service.ITradeTaskService;
import com.shuxiang.groupbuy.types.enums.ResponseCode;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 本机故障演练辅助接口（仅 dev profile）
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/dev/chaos/")
@Profile("dev")
public class ChaosDevController {

    @Resource
    private ITradeTaskService tradeTaskService;

    /**
     * 手动触发 notify_task 补偿（MQ 恢复后调用）
     */
    @PostMapping("run-notify-job")
    public Response<Map<String, Integer>> runNotifyJob() {
        try {
            Map<String, Integer> result = tradeTaskService.execNotifyJob();
            log.info("dev chaos 手动补偿 notify_task result:{}", JSON.toJSONString(result));
            return Response.<Map<String, Integer>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("dev chaos 手动补偿失败", e);
            return Response.<Map<String, Integer>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        }
    }
}
