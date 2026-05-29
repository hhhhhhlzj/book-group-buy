package com.shuxiang.groupbuy.config;

import com.shuxiang.groupbuy.api.response.Response;
import com.shuxiang.groupbuy.types.enums.ResponseCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 拼团锁单接口业务指标：gbm.lock.order
 */
@Aspect
@Component
public class LockOrderMetricsAspect {

    private final Counter successCounter;
    private final Counter failCounter;
    private final Timer lockTimer;

    public LockOrderMetricsAspect(MeterRegistry meterRegistry) {
        this.successCounter = Counter.builder("gbm.lock.order")
                .tag("result", "success")
                .description("拼团锁单成功次数")
                .register(meterRegistry);
        this.failCounter = Counter.builder("gbm.lock.order")
                .tag("result", "fail")
                .description("拼团锁单失败或业务拒绝次数")
                .register(meterRegistry);
        this.lockTimer = Timer.builder("gbm.lock.order.duration")
                .description("拼团锁单耗时")
                .register(meterRegistry);
    }

    @Around("execution(* com.shuxiang.groupbuy.trigger.http.MarketTradeController.lockMarketPayOrder(..))")
    public Object aroundLock(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start();
        try {
            Object result = joinPoint.proceed();
            if (result instanceof Response) {
                Response<?> response = (Response<?>) result;
                if (ResponseCode.SUCCESS.getCode().equals(response.getCode())) {
                    successCounter.increment();
                } else {
                    failCounter.increment();
                }
            } else {
                failCounter.increment();
            }
            return result;
        } catch (Throwable ex) {
            failCounter.increment();
            throw ex;
        } finally {
            sample.stop(lockTimer);
        }
    }
}
