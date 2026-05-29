package com.shuxiang.groupbuy.types.support;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 链路 traceId 常量与 MDC 工具（HTTP / 日志 / 出站调用共用）。
 */
public final class TraceIdSupport {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "trace-id";

    private TraceIdSupport() {
    }

    public static String resolve(String incoming) {
        if (incoming != null && !incoming.isBlank()) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }

    public static void put(String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            MDC.put(MDC_KEY, traceId);
        }
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
