package com.recoverai.backend.controller;

import com.recoverai.backend.config.RequestIdFilter;
import com.recoverai.backend.dto.ApiResponse;
import com.recoverai.backend.dto.WebhookIngestionResponse;
import com.recoverai.backend.service.WebhookIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookIngestionService webhookIngestionService;

    public WebhookController(WebhookIngestionService webhookIngestionService) {
        this.webhookIngestionService = webhookIngestionService;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<ApiResponse<WebhookIngestionResponse>> handleRazorpayWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader,
            @RequestHeader(value = "X-Merchant-ID", required = false) UUID merchantIdHeader,
            @RequestBody String rawPayload,
            HttpServletRequest request) {

        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTR);
        WebhookIngestionResponse response = webhookIngestionService.processWebhook(rawPayload, signatureHeader, merchantIdHeader, requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
