-- Flyway Initial Database Migration V1__initial_schema.sql

-- 1. Merchants Table
CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    auto_recovery_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_retry_count INT NOT NULL DEFAULT 3,
    min_recovery_probability DECIMAL(5,4) NOT NULL DEFAULT 0.6000,
    automatic_action_limit DECIMAL(19,4) NOT NULL DEFAULT 50000.0000,
    human_approval_threshold DECIMAL(19,4) NOT NULL DEFAULT 100000.0000,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 2. Customers Table
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    external_customer_id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    total_transactions INT NOT NULL DEFAULT 0,
    successful_transactions INT NOT NULL DEFAULT 0,
    failed_transactions INT NOT NULL DEFAULT 0,
    total_spend DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_customers_merchant_external_id UNIQUE (merchant_id, external_customer_id)
);

-- 3. Payments Table
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    razorpay_payment_id VARCHAR(255),
    razorpay_order_id VARCHAR(255),
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(50) NOT NULL,
    method VARCHAR(50),
    failure_code VARCHAR(100),
    failure_reason TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Indexes on Payments table
CREATE INDEX idx_payments_merchant ON payments(merchant_id);
CREATE INDEX idx_payments_customer ON payments(customer_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);
CREATE INDEX idx_payments_rzp_id ON payments(razorpay_payment_id);

-- 4. Recovery Cases Table
CREATE TABLE recovery_cases (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL UNIQUE REFERENCES payments(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    recovery_probability DECIMAL(5,4),
    diagnosis TEXT,
    expected_recovery_value DECIMAL(19,4),
    recommended_action VARCHAR(50),
    current_action VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);

-- 5. Recovery Attempts Table
CREATE TABLE recovery_attempts (
    id UUID PRIMARY KEY,
    recovery_case_id UUID NOT NULL REFERENCES recovery_cases(id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL,
    action_payload TEXT,
    policy_result TEXT,
    approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    executed_at TIMESTAMP WITH TIME ZONE,
    outcome VARCHAR(50) NOT NULL,
    recovered_amount DECIMAL(19,4),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 6. Agent Decisions Table
CREATE TABLE agent_decisions (
    id UUID PRIMARY KEY,
    recovery_case_id UUID NOT NULL REFERENCES recovery_cases(id) ON DELETE CASCADE,
    model_version VARCHAR(50),
    model_probability DECIMAL(5,4),
    diagnosis TEXT,
    candidate_actions TEXT,
    selected_action VARCHAR(50),
    reasoning_summary TEXT,
    policy_checks TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 7. Recovery Policies Table
CREATE TABLE recovery_policies (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL UNIQUE REFERENCES merchants(id) ON DELETE CASCADE,
    max_retry_count INT NOT NULL DEFAULT 3,
    min_recovery_probability DECIMAL(5,4) NOT NULL DEFAULT 0.6000,
    automatic_action_limit DECIMAL(19,4) NOT NULL DEFAULT 50000.0000,
    human_approval_threshold DECIMAL(19,4) NOT NULL DEFAULT 100000.0000,
    cooldown_minutes INT NOT NULL DEFAULT 60,
    auto_recovery_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 8. Audit Logs Table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    actor_type VARCHAR(50) NOT NULL,
    actor_id VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    reason TEXT,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 9. Webhook Events Table
CREATE TABLE webhook_events (
    id UUID PRIMARY KEY,
    razorpay_event_id VARCHAR(255) NOT NULL CONSTRAINT uk_webhook_events_rzp_event_id UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT,
    signature_valid BOOLEAN NOT NULL DEFAULT FALSE,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);
