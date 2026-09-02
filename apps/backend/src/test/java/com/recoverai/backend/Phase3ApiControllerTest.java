package com.recoverai.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.domain.entity.AuditLog;
import com.recoverai.backend.domain.enums.PaymentStatus;
import com.recoverai.backend.domain.enums.RecoveryCaseStatus;
import com.recoverai.backend.dto.*;
import com.recoverai.backend.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class Phase3ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    public void testMerchantLifecycleAndValidation() throws Exception {
        // 1. Create Merchant
        MerchantCreateRequest createRequest = new MerchantCreateRequest();
        createRequest.setName("Acme E-Commerce");
        createRequest.setCurrency("INR");

        MvcResult result = mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Acme E-Commerce"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ApiResponse<?> apiResponse = objectMapper.readValue(responseJson, ApiResponse.class);
        String merchantIdStr = objectMapper.convertValue(apiResponse.getData(), MerchantResponse.class).getId().toString();

        // 2. Retrieve Merchant by ID
        mockMvc.perform(get("/api/v1/merchants/" + merchantIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(merchantIdStr));

        // 3. Invalid Request - Blank Name
        MerchantCreateRequest invalidRequest = new MerchantCreateRequest();
        invalidRequest.setName("");

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    public void testCustomerLifecycleAndMerchantOwnership() throws Exception {
        // Create Merchant
        MerchantCreateRequest mReq = new MerchantCreateRequest();
        mReq.setName("Merchant For Customer Test");
        MvcResult mResult = mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mReq)))
                .andExpect(status().isCreated()).andReturn();

        UUID merchantId = objectMapper.readValue(mResult.getResponse().getContentAsString(), ApiResponse.class)
                .getData() != null ? UUID.fromString(((java.util.Map<?, ?>) objectMapper.readValue(mResult.getResponse().getContentAsString(), ApiResponse.class).getData()).get("id").toString()) : null;

        // 1. Create Customer
        CustomerCreateRequest cReq = new CustomerCreateRequest();
        cReq.setMerchantId(merchantId);
        cReq.setExternalCustomerId("CUST_EXT_2026");
        cReq.setName("Jane Smith");
        cReq.setEmail("jane@example.com");

        MvcResult cResult = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.externalCustomerId").value("CUST_EXT_2026"))
                .andReturn();

        UUID customerId = UUID.fromString(((java.util.Map<?, ?>) objectMapper.readValue(cResult.getResponse().getContentAsString(), ApiResponse.class).getData()).get("id").toString());

        // 2. Retrieve Customer
        mockMvc.perform(get("/api/v1/customers/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Jane Smith"));

        // 3. List Customers by Merchant
        mockMvc.perform(get("/api/v1/merchants/" + merchantId + "/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    @Test
    public void testSyntheticPaymentAndRecoveryCaseWorkflow() throws Exception {
        // Setup Merchant & Customer
        MerchantCreateRequest mReq = new MerchantCreateRequest();
        mReq.setName("Recovery Test Merchant");
        MvcResult mRes = mockMvc.perform(post("/api/v1/merchants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mReq))).andExpect(status().isCreated()).andReturn();
        UUID merchantId = UUID.fromString(((java.util.Map<?, ?>) objectMapper.readValue(mRes.getResponse().getContentAsString(), ApiResponse.class).getData()).get("id").toString());

        CustomerCreateRequest cReq = new CustomerCreateRequest();
        cReq.setMerchantId(merchantId);
        cReq.setExternalCustomerId("CUST_RECOVERY_01");
        MvcResult cRes = mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cReq))).andExpect(status().isCreated()).andReturn();
        UUID customerId = UUID.fromString(((java.util.Map<?, ?>) objectMapper.readValue(cRes.getResponse().getContentAsString(), ApiResponse.class).getData()).get("id").toString());

        // 1. Create Synthetic Payment (No Razorpay ID)
        PaymentCreateRequest pReq = new PaymentCreateRequest();
        pReq.setMerchantId(merchantId);
        pReq.setCustomerId(customerId);
        pReq.setAmount(new BigDecimal("2499.00"));
        pReq.setCurrency("INR");
        pReq.setStatus(PaymentStatus.FAILED);
        pReq.setMethod("upi");
        pReq.setFailureCode("BANK_TIMEOUT");
        pReq.setFailureReason("Temporary bank failure");

        MvcResult pRes = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.razorpayPaymentId").value(nullValue()))
                .andExpect(jsonPath("$.data.amount").value(2499.00))
                .andReturn();
        UUID paymentId = UUID.fromString(((java.util.Map<?, ?>) objectMapper.readValue(pRes.getResponse().getContentAsString(), ApiResponse.class).getData()).get("id").toString());

        // 2. Create Recovery Case (status OPEN)
        RecoveryCaseCreateRequest caseReq = new RecoveryCaseCreateRequest();
        caseReq.setMerchantId(merchantId);
        caseReq.setPaymentId(paymentId);

        MvcResult caseRes = mockMvc.perform(post("/api/v1/recovery/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.recoveryProbability").value(nullValue()))
                .andExpect(jsonPath("$.data.diagnosis").value(nullValue()))
                .andReturn();
        UUID caseId = UUID.fromString(((java.util.Map<?, ?>) objectMapper.readValue(caseRes.getResponse().getContentAsString(), ApiResponse.class).getData()).get("id").toString());

        // 3. Duplicate Recovery Case Rejection
        mockMvc.perform(post("/api/v1/recovery/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));

        // 4. Verify Audit Log Creation
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId("RecoveryCase", caseId);
        assertFalse(auditLogs.isEmpty());
        assertEquals("RECOVERY_CASE_CREATED", auditLogs.get(0).getEventType());
    }

    @Test
    public void testErrorHandlingAndPaginationLimits() throws Exception {
        // Non-existent payment retrieval
        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        // Invalid Payment Amount (<= 0)
        PaymentCreateRequest invalidPReq = new PaymentCreateRequest();
        invalidPReq.setMerchantId(UUID.randomUUID());
        invalidPReq.setCustomerId(UUID.randomUUID());
        invalidPReq.setAmount(new BigDecimal("-100.00"));
        invalidPReq.setStatus(PaymentStatus.FAILED);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        // Test Pagination Size Cap (Requesting 200 should cap size to 100)
        mockMvc.perform(get("/api/v1/merchants?page=0&size=200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(100));
    }
}
