package com.apigatewaypagos.demo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.apigatewaypagos.demo.infrastructure.persistence.entity.MerchantConfigEntity;

@Repository
public interface SpringDataMerchantConfigRepository extends JpaRepository<MerchantConfigEntity, String> {
}
