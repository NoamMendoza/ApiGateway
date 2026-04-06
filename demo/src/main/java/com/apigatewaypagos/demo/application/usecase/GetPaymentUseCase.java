package com.apigatewaypagos.demo.application.usecase;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.apigatewaypagos.demo.application.dto.PaymentResponseDTO;
import com.apigatewaypagos.demo.domain.exception.PaymentNotFoundException;
import com.apigatewaypagos.demo.domain.repository.PaymentRepository;

@Service
public class GetPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetPaymentUseCase.class);
    private final PaymentRepository paymentRepository;

    public GetPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponseDTO execute(UUID paymentId, String merchantId) {
        log.info("Consultando estado del pago — paymentId={}, merchantId={}", paymentId, merchantId);

        return paymentRepository.findById(paymentId)
            .filter(payment -> payment.getMerchantId().equals(merchantId))
            .map(payment -> {
                log.debug("Pago encontrado y autorizado para el comercio — paymentId={}, status={}", payment.getId(), payment.getStatus());
                return PaymentResponseDTO.fromDomain(payment);
            })
            .orElseThrow(() -> {
                log.warn("Pago no encontrado o sin acceso — paymentId={}, merchantId={}", paymentId, merchantId);
                return new PaymentNotFoundException("No se encontró el pago con ID: " + paymentId);
            });
    }
}
