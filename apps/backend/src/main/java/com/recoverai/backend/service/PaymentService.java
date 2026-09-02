package com.recoverai.backend.service;

import com.recoverai.backend.domain.entity.Customer;
import com.recoverai.backend.domain.entity.Merchant;
import com.recoverai.backend.domain.entity.Payment;
import com.recoverai.backend.domain.enums.ActorType;
import com.recoverai.backend.domain.enums.PaymentStatus;
import com.recoverai.backend.dto.PageResponse;
import com.recoverai.backend.dto.PaymentCreateRequest;
import com.recoverai.backend.dto.PaymentResponse;
import com.recoverai.backend.exception.InvalidRequestException;
import com.recoverai.backend.exception.ResourceNotFoundException;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    public PaymentService(PaymentRepository paymentRepository, MerchantRepository merchantRepository, CustomerRepository customerRepository, AuditLogService auditLogService) {
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
        this.customerRepository = customerRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        Merchant merchant = merchantRepository.findById(request.getMerchantId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + request.getMerchantId()));

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        if (!customer.getMerchant().getId().equals(merchant.getId())) {
            throw new InvalidRequestException("Customer does not belong to the specified merchant.");
        }

        Payment payment = new Payment();
        payment.setMerchant(merchant);
        payment.setCustomer(customer);
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpayOrderId(request.getRazorpayOrderId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setStatus(request.getStatus());
        payment.setMethod(request.getMethod());
        payment.setFailureCode(request.getFailureCode());
        payment.setFailureReason(request.getFailureReason());
        if (request.getRetryCount() != null) payment.setRetryCount(request.getRetryCount());

        payment = paymentRepository.save(payment);

        // Update customer statistics
        customer.setTotalTransactions(customer.getTotalTransactions() + 1);
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            customer.setSuccessfulTransactions(customer.getSuccessfulTransactions() + 1);
            customer.setTotalSpend(customer.getTotalSpend().add(payment.getAmount()));
        } else if (payment.getStatus() == PaymentStatus.FAILED) {
            customer.setFailedTransactions(customer.getFailedTransactions() + 1);
        }
        customerRepository.save(customer);

        auditLogService.logEvent(
                "Payment", payment.getId(), "PAYMENT_CREATED",
                ActorType.SYSTEM, "API", "CREATE_PAYMENT", "Payment recorded with status: " + payment.getStatus()
        );

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getPaymentsByMerchant(UUID merchantId, PaymentStatus status, UUID customerId, int page, int size) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new ResourceNotFoundException("Merchant not found with id: " + merchantId);
        }
        int pageSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), pageSize);

        List<PaymentResponse> filteredPayments = paymentRepository.findAll().stream()
                .filter(p -> p.getMerchant().getId().equals(merchantId))
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> customerId == null || p.getCustomer().getId().equals(customerId))
                .map(PaymentResponse::from)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredPayments.size());
        List<PaymentResponse> pageContent = (start <= filteredPayments.size()) ? filteredPayments.subList(start, end) : List.of();

        Page<PaymentResponse> paymentPage = new PageImpl<>(pageContent, pageable, filteredPayments.size());
        return PageResponse.from(paymentPage);
    }
}
