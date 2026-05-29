package com.shuxiang.groupbuy.config;

import com.shuxiang.groupbuy.infrastructure.dao.INotifyTaskDao;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * notify_task 待补偿条数（status in 0,2），用于 MQ 故障演练观测
 */
@Component
public class NotifyTaskMetricsScheduler {

    private final AtomicInteger pendingCount = new AtomicInteger(0);
    private final INotifyTaskDao notifyTaskDao;

    public NotifyTaskMetricsScheduler(MeterRegistry meterRegistry, INotifyTaskDao notifyTaskDao) {
        this.notifyTaskDao = notifyTaskDao;
        Gauge.builder("gbm_notify_task_pending", pendingCount, AtomicInteger::get)
                .description("notify_task 待执行/重试条数")
                .register(meterRegistry);
    }

    @PostConstruct
    public void init() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${group-buy.metrics.notify-task-refresh-ms:30000}")
    public void refresh() {
        Integer count = notifyTaskDao.countPendingNotifyTasks();
        pendingCount.set(count == null ? 0 : count);
    }
}
