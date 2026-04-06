package com.apigatewaypagos.demo.infrastructure.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apigatewaypagos.demo.application.dto.MetricsResponseDTO;
import com.apigatewaypagos.demo.infrastructure.SpringDataPaymentRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/admin/metrics")
public class MetricsController {
    private final SpringDataPaymentRepository metricsRepo;

    public MetricsController(SpringDataPaymentRepository metricsRepo) {
        this.metricsRepo = metricsRepo;
    }

    @GetMapping
    @Cacheable(value ="metrics", key = "#root.methodName + '_' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName()")
    public MetricsResponseDTO getMetrics() {
        String merchantId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Calculando métricas privadas para el comercio: {}", merchantId);

        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);

        List<Object[]> statusCounts = metricsRepo.countByStatus(merchantId);
        Map<String, Long> distribution = new HashMap<>();
        for (Object[] row : statusCounts) {
            distribution.put(row[0].toString(), (Long) row[1]);
        }

        BigDecimal revenue = metricsRepo.sumCapturedAmountSince(merchantId, startOfDay);
        long totalToday = metricsRepo.countCreatedSince(merchantId, startOfDay);

        long captured = distribution.getOrDefault("CAPTURED", 0L);
        double successRate = totalToday > 0 ? (double) captured / totalToday * 100 : 0;

        return new MetricsResponseDTO(
            totalToday,
            revenue,
            successRate,
            0,
            distribution
        );
    }
    
}
