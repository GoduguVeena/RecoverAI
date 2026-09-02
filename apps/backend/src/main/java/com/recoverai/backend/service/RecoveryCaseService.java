package com.recoverai.backend.service;

import com.recoverai.backend.domain.entity.Payment;
import com.recoverai.backend.domain.entity.RecoveryCase;
import com.recoverai.backend.domain.enums.ActorType;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;
import com.recoverai.backend.dto.PageResponse;
import com.recoverai.backend.dto.RecoveryCaseCreateRequest;
import com.recoverai.backend.dto.RecoveryCaseResponse;
import com.recoverai.backend.exception.DuplicateResourceException;
import com.recoverai.backend.exception.InvalidRequestException;
import com.recoverai.backend.exception.ResourceNotFoundException;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecoveryCaseService {

    private final RecoveryCaseRepository recoveryCaseRepository;
    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final AuditLogService auditLogService;

    public RecoveryCaseService(RecoveryCaseRepository recoveryCaseRepository, PaymentRepository paymentRepository, MerchantRepository merchantRepository, AuditLogService auditLogService) {
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public RecoveryCaseResponse createRecoveryCase(RecoveryCaseCreateRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + request.getPaymentId()));

        if (!payment.getMerchant().getId().equals(request.getMerchantId())) {
            throw new InvalidRequestException("Payment does not belong to the specified merchant.");
        }

        Optional<RecoveryCase> existingCase = recoveryCaseRepository.findByPaymentId(payment.getId());
        if (existingCase.isPresent()) {
            throw new DuplicateResourceException("An active recovery case already exists for payment id: " + payment.getId());
        }

        RecoveryCase recoveryCase = new RecoveryCase();
        recoveryCase.setPayment(payment);
        recoveryCase.setStatus(RecoveryCaseStatus.OPEN);

        recoveryCase = recoveryCaseRepository.save(recoveryCase);

        auditLogService.logEvent(
                "RecoveryCase", recoveryCase.getId(), "RECOVERY_CASE_CREATED",
                ActorType.SYSTEM, "API", "CREATE_RECOVERY_CASE", "Recovery case opened for failed payment"
        );

        return RecoveryCaseResponse.from(recoveryCase);
    }

    @Transactional(readOnly = true)
    public RecoveryCaseResponse getRecoveryCaseById(UUID id) {
        RecoveryCase recoveryCase = recoveryCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recovery case not found with id: " + id));
        return RecoveryCaseResponse.from(recoveryCase);
    }

    @Transactional(readOnly = true)
    public PageResponse<RecoveryCaseResponse> getRecoveryCases(UUID merchantId, RecoveryCaseStatus status, int page, int size) {
        int pageSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), pageSize);

        List<RecoveryCaseResponse> filteredCases = recoveryCaseRepository.findAll().stream()
                .filter(c -> merchantId == null || c.getPayment().getMerchant().getId().equals(merchantId))
                .filter(c -> status == null || c.getStatus() == status)
                .map(RecoveryCaseResponse::from)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredCases.size());
        List<RecoveryCaseResponse> pageContent = (start <= filteredCases.size()) ? filteredCases.subList(start, end) : List.of();

        Page<RecoveryCaseResponse> casePage = new PageImpl<>(pageContent, pageable, filteredCases.size());
        return PageResponse.from(casePage);
    }
}
