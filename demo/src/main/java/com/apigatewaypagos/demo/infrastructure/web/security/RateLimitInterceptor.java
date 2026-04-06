package com.apigatewaypagos.demo.infrastructure.web.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final long MAX_REQUESTS = 5;
    private static final long WINDOW_MINUTES = 1;

    private final StringRedisTemplate stringRedisTemplate;

    public RateLimitInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apiKey = request.getHeader("X-API-KEY");
        String method = request.getMethod();
        
        // Las consultas (GET) soportan más tráfico por el dashboard, los cobros (POST) se limitan estrictamente.
        long maxRequests = method.equalsIgnoreCase("GET") ? 60 : 5;
        
        String redisKey = "rate_limit:" + method + ":" + apiKey;
        Long counter = stringRedisTemplate.opsForValue().increment(redisKey, 1);

        if (counter != null && counter == 1) {
            stringRedisTemplate.expire(redisKey, WINDOW_MINUTES, TimeUnit.MINUTES);
        }

        if (counter != null && counter > maxRequests) {
            log.warn("Rate limit excedido — apiKey={}, method={}, contador={}/{}, path={}",
                    maskKey(apiKey), method, counter, maxRequests, request.getRequestURI());
            response.setStatus(429);
            return false;
        }

        log.debug("Request aceptado — apiKey={}, contador={}/{}, path={}",
                maskKey(apiKey), counter, MAX_REQUESTS, request.getRequestURI());
        return true;
    }

    /**
     * Oculta la mayoría de la API Key en el log para no exponerla en registros.
     * "sk_test-123456789" → "sk_te...6789"
     */
    private String maskKey(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 6) + "..." + key.substring(key.length() - 4);
    }
}
