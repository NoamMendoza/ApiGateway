package com.apigatewaypagos.demo.infrastructure.web.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.stream.Collectors;
import java.util.Arrays;
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

        if (!path.startsWith("/api/payments") || path.startsWith("/api/webhooks") || path.startsWith("/api/admin/metrics")) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedApiKey = request.getHeader("X-API-KEY");

        if (providedApiKey == null || providedApiKey.trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<ApiKeyService.ApiKeyResult> validate = apiKeyService.validateAndGetApiKeyDetails(providedApiKey);

        if (validate.isPresent()) {
            ApiKeyService.ApiKeyResult keyDetails = validate.get();
            var authorities = Arrays.stream(keyDetails.scopes().split(","))
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope.trim().toUpperCase()))
                .collect(Collectors.toList());

            var auth = new UsernamePasswordAuthenticationToken(keyDetails.merchantId(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);

        }else{
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("API key invalida o ausente");
        }
    }
}
