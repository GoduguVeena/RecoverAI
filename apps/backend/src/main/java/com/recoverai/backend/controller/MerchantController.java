package com.recoverai.backend.controller;

import com.recoverai.backend.dto.ApiResponse;
import com.recoverai.backend.dto.MerchantCreateRequest;
import com.recoverai.backend.dto.MerchantResponse;
import com.recoverai.backend.dto.PageResponse;
import com.recoverai.backend.service.MerchantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MerchantResponse>> createMerchant(@Valid @RequestBody MerchantCreateRequest request) {
        MerchantResponse response = merchantService.createMerchant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantResponse>> getMerchantById(@PathVariable UUID id) {
        MerchantResponse response = merchantService.getMerchantById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MerchantResponse>>> getMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<MerchantResponse> response = merchantService.getMerchants(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
