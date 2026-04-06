package com.apigatewaypagos.demo.infrastructure;

import org.springframework.stereotype.Component;

import com.apigatewaypagos.demo.domain.model.Payment;
import com.apigatewaypagos.demo.domain.repository.PaymentRepository;
import com.apigatewaypagos.demo.infrastructure.persistence.entity.PaymentEntity;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
        return springDataPaymentRepository.findById(id).map(PaymentEntity::toDomain);
    }

    @Override
    public java.util.Optional<Payment> findByIdempotencyKey(String idempotencyKey){
        return springDataPaymentRepository.findByIdempotencyKey(idempotencyKey).map(PaymentEntity::toDomain);
    }

    @Override
    public java.util.Optional<Payment> findByExternalTransactionId(String externalTransactionId){
        return springDataPaymentRepository.findByExternalTransactionId(externalTransactionId).map(PaymentEntity::toDomain);
    }

    @Override
    public java.util.List<Payment> findByMerchantId(
        String merchantId, 
        String paymentId,
        com.apigatewaypagos.demo.domain.model.PaymentStatus status,
        java.time.LocalDateTime startDate,
        java.time.LocalDateTime endDate,
        java.math.BigDecimal minAmount,
        java.math.BigDecimal maxAmount,
        int page, 
        int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        org.springframework.data.jpa.domain.Specification<PaymentEntity> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            
            predicates.add(cb.equal(root.get("merchantId"), merchantId));
            
            if (paymentId != null && !paymentId.isBlank()) {
                predicates.add(cb.like(root.get("id").as(String.class), "%" + paymentId + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return springDataPaymentRepository.findAll(spec, pageable)
                .stream()
                .map(PaymentEntity::toDomain)
                .toList();
    }
}

