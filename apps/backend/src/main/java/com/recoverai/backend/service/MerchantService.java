package com.recoverai.backend.service;

import com.recoverai.backend.domain.entity.Merchant;
import com.recoverai.backend.domain.enums.ActorType;
import com.recoverai.backend.dto.MerchantCreateRequest;
import com.recoverai.backend.dto.MerchantResponse;
import com.recoverai.backend.dto.PageResponse;
import com.recoverai.backend.exception.ResourceNotFoundException;
import com.recoverai.backend.repository.MerchantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final AuditLogService auditLogService;

    public MerchantService(MerchantRepository merchantRepository, AuditLogService auditLogService) {
        this.merchantRepository = merchantRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public MerchantResponse createMerchant(MerchantCreateRequest request) {
        Merchant merchant = new Merchant();
        merchant.setName(request.getName());
        if (request.getCurrency() != null) merchant.setCurrency(request.getCurrency());
        if (request.getAutoRecoveryEnabled() != null) merchant.setAutoRecoveryEnabled(request.getAutoRecoveryEnabled());
        if (request.getMaxRetryCount() != null) merchant.setMaxRetryCount(request.getMaxRetryCount());
        if (request.getMinRecoveryProbability() != null) merchant.setMinRecoveryProbability(request.getMinRecoveryProbability());
        if (request.getAutomaticActionLimit() != null) merchant.setAutomaticActionLimit(request.getAutomaticActionLimit());
        if (request.getHumanApprovalThreshold() != null) merchant.setHumanApprovalThreshold(request.getHumanApprovalThreshold());

        merchant = merchantRepository.save(merchant);

        auditLogService.logEvent(
                "Merchant", merchant.getId(), "MERCHANT_CREATED",
                ActorType.SYSTEM, "API", "CREATE_MERCHANT", "Merchant onboarded"
        );

        return MerchantResponse.from(merchant);
    }

    @Transactional(readOnly = true)
    public MerchantResponse getMerchantById(UUID id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + id));
        return MerchantResponse.from(merchant);
    }

    @Transactional(readOnly = true)
    public PageResponse<MerchantResponse> getMerchants(int page, int size) {
        int pageSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), pageSize);
        Page<MerchantResponse> merchantPage = merchantRepository.findAll(pageable).map(MerchantResponse::from);
        return PageResponse.from(merchantPage);
    }
}
