package com.apigatewaypagos.demo.infrastructure.persistence;

import java.util.Optional;
import org.springframework.stereotype.Component;

import com.apigatewaypagos.demo.domain.model.ApiKey;
import com.apigatewaypagos.demo.domain.repository.ApiKeyRepository;
import com.apigatewaypagos.demo.infrastructure.persistence.entity.ApiKeyEntity;

@Component
public class ApiKeyRepositoryAdapter implements ApiKeyRepository {
    
    private final SpringDataApiKeyRepository springDataApiKeyRepository;

    public ApiKeyRepositoryAdapter(SpringDataApiKeyRepository springDataApiKeyRepository){
        this.springDataApiKeyRepository = springDataApiKeyRepository;
    }

    @Override
    public Optional<ApiKey> findByKeyPrefix(String keyPrefix) {
        return springDataApiKeyRepository.findByKeyPrefix(keyPrefix)
            .map(ApiKeyEntity::toDomain);
    }

    @Override
    public void save(ApiKey apiKey) {
        ApiKeyEntity entity = ApiKeyEntity.fromDomain(apiKey);
        springDataApiKeyRepository.save(entity);
    }
}
