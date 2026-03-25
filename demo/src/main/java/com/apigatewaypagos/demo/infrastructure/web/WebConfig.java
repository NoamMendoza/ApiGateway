package com.apigatewaypagos.demo.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import com.apigatewaypagos.demo.infrastructure.web.security.RateLimitInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer{
    //inyectar RateLimitInterceptor
    private RateLimitInterceptor rateLimitInterceptor;
    public WebConfig(RateLimitInterceptor rateLimitInterceptor){
        this.rateLimitInterceptor = rateLimitInterceptor;
    }
    //Sobrescribe el método public void addInterceptors(InterceptorRegistry registry) y adentro pon: registry.addInterceptor(tuVariableInterceptor).addPathPatterns("/api/payments/**");
    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/payments/**");
    }
}
