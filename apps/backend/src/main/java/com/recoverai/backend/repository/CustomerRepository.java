package com.recoverai.backend.repository;

import com.recoverai.backend.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByMerchantIdAndExternalCustomerId(UUID merchantId, String externalCustomerId);
}
