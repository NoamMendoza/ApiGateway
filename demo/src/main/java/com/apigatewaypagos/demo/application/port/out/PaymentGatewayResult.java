package com.apigatewaypagos.demo.application.port.out;

public record PaymentGatewayResult(boolean isSuccess, String externalId, String errorMessage) {
    public static PaymentGatewayResult success(String externalId) {
        return new PaymentGatewayResult(true, externalId, null);
    }
    public static PaymentGatewayResult failure(String errorMessage) {
        return new PaymentGatewayResult(false, null, errorMessage);
    }
}
