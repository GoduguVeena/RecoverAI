package com.recoverai.backend.controller;

import com.recoverai.backend.config.RequestIdFilter;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;
import com.recoverai.backend.dto.ApiResponse;
import com.recoverai.backend.dto.PageResponse;
import com.recoverai.backend.dto.RecoveryAnalysisResponse;
import com.recoverai.backend.dto.RecoveryCaseCreateRequest;
import com.recoverai.backend.dto.RecoveryCaseResponse;
import com.recoverai.backend.dto.RecoveryExecutionResponse;
import com.recoverai.backend.service.RecoveryAnalysisService;
import com.recoverai.backend.service.RecoveryCaseService;
import com.recoverai.backend.service.RecoveryExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recovery/cases")
public class RecoveryCaseController {

    private final RecoveryCaseService recoveryCaseService;
    private final RecoveryAnalysisService recoveryAnalysisService;
    private final RecoveryExecutionService recoveryExecutionService;

    public RecoveryCaseController(RecoveryCaseService recoveryCaseService,
                                 RecoveryAnalysisService recoveryAnalysisService,
                                 RecoveryExecutionService recoveryExecutionService) {
        this.recoveryCaseService = recoveryCaseService;
        this.recoveryAnalysisService = recoveryAnalysisService;
        this.recoveryExecutionService = recoveryExecutionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecoveryCaseResponse>> createRecoveryCase(@Valid @RequestBody RecoveryCaseCreateRequest request) {
        RecoveryCaseResponse response = recoveryCaseService.createRecoveryCase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecoveryCaseResponse>> getRecoveryCaseById(@PathVariable UUID id) {
        RecoveryCaseResponse response = recoveryCaseService.getRecoveryCaseById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecoveryCaseResponse>>> getRecoveryCases(
            @RequestParam(required = false) UUID merchantId,
            @RequestParam(required = false) RecoveryCaseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<RecoveryCaseResponse> response = recoveryCaseService.getRecoveryCases(merchantId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<ApiResponse<RecoveryAnalysisResponse>> analyzeRecoveryCase(@PathVariable UUID id) {
        RecoveryAnalysisResponse response = recoveryAnalysisService.analyzeRecoveryCase(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<RecoveryExecutionResponse>> executeRecoveryAction(
            @PathVariable UUID id,
            HttpServletRequest request) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        RecoveryExecutionResponse response = recoveryExecutionService.executeRecoveryAction(id, requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
