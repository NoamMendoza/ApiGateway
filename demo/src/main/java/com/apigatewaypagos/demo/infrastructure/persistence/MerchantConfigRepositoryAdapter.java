package com.apigatewaypagos.demo.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.apigatewaypagos.demo.domain.model.MerchantConfig;
import com.apigatewaypagos.demo.domain.repository.MerchantConfigRepository;
import com.apigatewaypagos.demo.infrastructure.persistence.entity.MerchantConfigEntity;

@Component
public class MerchantConfigRepositoryAdapter implements MerchantConfigRepository {

    private final SpringDataMerchantConfigRepository repository;

    public MerchantConfigRepositoryAdapter(SpringDataMerchantConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MerchantConfig> findByMerchantId(String merchantId) {
        return repository.findById(merchantId).map(MerchantConfigEntity::toDomain);
    }

    @Override
    public void save(MerchantConfig merchantConfig) {
        MerchantConfigEntity entity = MerchantConfigEntity.fromDomain(merchantConfig);
        repository.save(entity);
    }
}
