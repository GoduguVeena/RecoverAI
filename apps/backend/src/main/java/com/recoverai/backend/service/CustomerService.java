package com.recoverai.backend.service;

import com.recoverai.backend.domain.entity.Customer;
import com.recoverai.backend.domain.entity.Merchant;
import com.recoverai.backend.domain.enums.ActorType;
import com.recoverai.backend.dto.CustomerCreateRequest;
import com.recoverai.backend.dto.CustomerResponse;
import com.recoverai.backend.dto.PageResponse;
import com.recoverai.backend.exception.DuplicateResourceException;
import com.recoverai.backend.exception.ResourceNotFoundException;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
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
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;
    private final AuditLogService auditLogService;

    public CustomerService(CustomerRepository customerRepository, MerchantRepository merchantRepository, AuditLogService auditLogService) {
        this.customerRepository = customerRepository;
        this.merchantRepository = merchantRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        Merchant merchant = merchantRepository.findById(request.getMerchantId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + request.getMerchantId()));

        if (customerRepository.findByMerchantIdAndExternalCustomerId(request.getMerchantId(), request.getExternalCustomerId()).isPresent()) {
            throw new DuplicateResourceException("Customer with external ID " + request.getExternalCustomerId() + " already exists for this merchant.");
        }

        Customer customer = new Customer();
        customer.setMerchant(merchant);
        customer.setExternalCustomerId(request.getExternalCustomerId());
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());

        customer = customerRepository.save(customer);

        auditLogService.logEvent(
                "Customer", customer.getId(), "CUSTOMER_CREATED",
                ActorType.SYSTEM, "API", "CREATE_CUSTOMER", "Customer registered for merchant"
        );

        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomersByMerchant(UUID merchantId, int page, int size) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new ResourceNotFoundException("Merchant not found with id: " + merchantId);
        }
        int pageSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), pageSize);

        // Simple paginated fetch
        List<CustomerResponse> allCustomers = customerRepository.findAll().stream()
                .filter(c -> c.getMerchant().getId().equals(merchantId))
                .map(CustomerResponse::from)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allCustomers.size());
        List<CustomerResponse> pageContent = (start <= allCustomers.size()) ? allCustomers.subList(start, end) : List.of();

        Page<CustomerResponse> customerPage = new PageImpl<>(pageContent, pageable, allCustomers.size());
        return PageResponse.from(customerPage);
    }
}
