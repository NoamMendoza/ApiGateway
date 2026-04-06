package com.apigatewaypagos.demo.application.dto;

import java.math.BigDecimal;
import java.util.Map;

public record MetricsResponseDTO (
    long totalToday,
    BigDecimal revenueToday,
    double successRate,
    long activeDisputes,
    Map<String, Long> statusDistribution
) implements java.io.Serializable {}
