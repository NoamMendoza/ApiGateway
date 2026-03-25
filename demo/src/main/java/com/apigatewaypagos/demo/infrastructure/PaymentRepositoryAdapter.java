package com.apigatewaypagos.demo.infrastructure;

import org.springframework.stereotype.Component;

import com.apigatewaypagos.demo.domain.model.Payment;
import com.apigatewaypagos.demo.domain.repository.PaymentRepository;
import com.apigatewaypagos.demo.infrastructure.persistence.entity.PaymentEntity;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {
    private SpringDataPaymentRepository springDataPaymentRepository;

    public PaymentRepositoryAdapter(SpringDataPaymentRepository springDataPaymentRepository){
        this.springDataPaymentRepository = springDataPaymentRepository;
    }

    @Override
    public void save(Payment payment){
        PaymentEntity entity = PaymentEntity.fromDomain(payment);

        springDataPaymentRepository.save(entity);
    }

    @Override
    public java.util.Optional<Payment> findById(java.util.UUID id){
        //TODO: Implementar busqueda en base de datos mas adelante
        return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Payment> findByIdempotencyKey(String idempotencyKey){
        return springDataPaymentRepository.findByIdempotencyKey(idempotencyKey).map(PaymentEntity::toDomain);
    }
}
