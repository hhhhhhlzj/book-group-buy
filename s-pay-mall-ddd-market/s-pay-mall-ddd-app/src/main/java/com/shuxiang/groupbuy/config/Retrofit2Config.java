package com.shuxiang.groupbuy.config;

import com.shuxiang.groupbuy.infrastructure.gateway.IGroupBuyMarketService;
import com.shuxiang.groupbuy.infrastructure.gateway.IWeixinApiService;
import com.shuxiang.groupbuy.types.support.TraceIdSupport;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
@Configuration
public class Retrofit2Config {

    @Value("${app.config.group-buy-market.api-url}")
    private String groupBuyMarketApiUrl;

    private static final Interceptor TRACE_ID_INTERCEPTOR = chain -> {
        Request request = chain.request();
        String traceId = TraceIdSupport.current();
        if (traceId != null && !traceId.isBlank()) {
            request = request.newBuilder()
                    .addHeader(TraceIdSupport.HEADER, traceId)
                    .build();
        }
        return chain.proceed(request);
    };

    @Bean
    public IWeixinApiService weixinApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weixin.qq.com/")
                .addConverterFactory(JacksonConverterFactory.create()).build();

        return retrofit.create(IWeixinApiService.class);
    }

    @Bean
    public IGroupBuyMarketService groupBuyMarketService() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(TRACE_ID_INTERCEPTOR)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(groupBuyMarketApiUrl)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create()).build();

        return retrofit.create(IGroupBuyMarketService.class);
    }

}
