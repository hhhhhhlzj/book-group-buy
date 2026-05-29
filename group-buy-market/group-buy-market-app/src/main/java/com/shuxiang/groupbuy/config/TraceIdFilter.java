package com.shuxiang.groupbuy.config;

import com.shuxiang.groupbuy.types.support.TraceIdSupport;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = TraceIdSupport.resolve(request.getHeader(TraceIdSupport.HEADER));
        TraceIdSupport.put(traceId);
        response.setHeader(TraceIdSupport.HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceIdSupport.clear();
        }
    }

}
