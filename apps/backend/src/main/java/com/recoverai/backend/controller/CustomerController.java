package com.recoverai.backend.controller;

import com.recoverai.backend.dto.ApiResponse;
import com.recoverai.backend.dto.CustomerCreateRequest;
import com.recoverai.backend.dto.CustomerResponse;
import com.recoverai.backend.dto.PageResponse;
import com.recoverai.backend.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/customers")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable UUID id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/merchants/{merchantId}/customers")
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> getCustomersByMerchant(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<CustomerResponse> response = customerService.getCustomersByMerchant(merchantId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
