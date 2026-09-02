package com.recoverai.backend.controller;

import com.recoverai.backend.domain.enums.PaymentStatus;
import com.recoverai.backend.dto.ApiResponse;
import com.recoverai.backend.dto.PageResponse;
import com.recoverai.backend.dto.PaymentCreateRequest;
import com.recoverai.backend.dto.PaymentResponse;
import com.recoverai.backend.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable UUID id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/merchants/{merchantId}/payments")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getPaymentsByMerchant(
            @PathVariable UUID merchantId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<PaymentResponse> response = paymentService.getPaymentsByMerchant(merchantId, status, customerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
