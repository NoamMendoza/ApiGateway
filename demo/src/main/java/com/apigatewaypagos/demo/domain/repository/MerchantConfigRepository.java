package com.apigatewaypagos.demo.domain.repository;

import java.util.Optional;
import com.apigatewaypagos.demo.domain.model.MerchantConfig;

public interface MerchantConfigRepository {
    Optional<MerchantConfig> findByMerchantId(String merchantId);
    void save(MerchantConfig merchantConfig);
}
