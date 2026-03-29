package com.apigatewaypagos.demo.infrastructure.web.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.apigatewaypagos.demo.application.service.ApiKeyService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter{
    private ApiKeyService apiKeyService;

    public ApiKeyAuthFilter(ApiKeyService apiKeyService){
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException{
        String path = request.getRequestURI();

        if(!path.startsWith("/api/payments")){
            filterChain.doFilter(request, response);
            return;
        }

        String providedApiKey = request.getHeader("X-API-KEY");

        Optional<String> validate = apiKeyService.validateAndGetMerchantId(providedApiKey);

        if (validate.isPresent()) {
            var auth = new UsernamePasswordAuthenticationToken(validate.get(), null, null);
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);

        }else{
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("API key invalida o ausente");
        }
    }
}
