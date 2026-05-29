package com.shuxiang.groupbuy.config;

import com.shuxiang.groupbuy.domain.order.adapter.repository.IOrderRepository;
import com.shuxiang.groupbuy.domain.order.model.entity.OrderEntity;
import com.shuxiang.groupbuy.domain.order.model.valobj.OrderStatusVO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 支付成功处理指标：mall.alipay.notify（切片4 重复回调演练）
 */
@Aspect
@Component
public class PayNotifyMetricsAspect {

    private final Counter successCounter;
    private final Counter duplicateCounter;
    private final Counter notFoundCounter;
    private final IOrderRepository orderRepository;

    public PayNotifyMetricsAspect(MeterRegistry meterRegistry, IOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        this.successCounter = Counter.builder("mall.alipay.notify")
                .tag("result", "success")
                .description("支付回调首次处理成功")
                .register(meterRegistry);
        this.duplicateCounter = Counter.builder("mall.alipay.notify")
                .tag("result", "duplicate")
                .description("支付回调幂等跳过")
                .register(meterRegistry);
        this.notFoundCounter = Counter.builder("mall.alipay.notify")
                .tag("result", "not_found")
                .description("支付回调订单不存在")
                .register(meterRegistry);
    }

    @Around("execution(* com.shuxiang.groupbuy.domain.order.service.OrderService.changeOrderPaySuccess(..))")
    public Object aroundPaySuccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String orderId = (String) joinPoint.getArgs()[0];
        OrderEntity order = orderRepository.queryOrderByOrderId(orderId);
        if (order == null) {
            notFoundCounter.increment();
            return joinPoint.proceed();
        }
        String status = order.getOrderStatusVO().getCode();
        if (OrderStatusVO.PAY_SUCCESS.getCode().equals(status)
                || OrderStatusVO.DEAL_DONE.getCode().equals(status)) {
            duplicateCounter.increment();
            return joinPoint.proceed();
        }
        joinPoint.proceed();
        successCounter.increment();
        return null;
    }
}
