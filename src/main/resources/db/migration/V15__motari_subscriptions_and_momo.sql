-- ============================================================
-- V15: Motari Subscriptions & MTN MoMo Financial Ledger
-- ============================================================

-- 1. Create Subscription Plans Table
CREATE TABLE subscription_plans (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    duration_days INT NOT NULL,
    price_rwf DECIMAL(12,2) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_popular BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create Motari Subscriptions Table
CREATE TABLE motari_subscriptions (
    id UUID PRIMARY KEY,
    motari_id UUID NOT NULL REFERENCES motaris(id) ON DELETE CASCADE,
    plan_id VARCHAR(50) NOT NULL REFERENCES subscription_plans(id),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    amount_paid DECIMAL(12,2) NOT NULL,
    start_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    grace_period_end_date TIMESTAMP WITHOUT TIME ZONE,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'MTN_MOMO',
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_motari_subscriptions_motari ON motari_subscriptions(motari_id);
CREATE INDEX idx_motari_subscriptions_status ON motari_subscriptions(status);
CREATE INDEX idx_motari_subscriptions_expiry ON motari_subscriptions(expiry_date);

-- 3. Create MoMo Financial Transactions Ledger Table
CREATE TABLE momo_transactions (
    id UUID PRIMARY KEY,
    momo_transaction_id VARCHAR(100) NOT NULL UNIQUE,
    external_reference VARCHAR(150) NOT NULL UNIQUE,
    financial_transaction_id VARCHAR(100),
    motari_id UUID NOT NULL REFERENCES motaris(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES motari_subscriptions(id) ON DELETE SET NULL,
    plan_id VARCHAR(50) REFERENCES subscription_plans(id),
    phone_number VARCHAR(30) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'RWF',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_gateway VARCHAR(50) NOT NULL DEFAULT 'MTN_MOMO',
    payer_note VARCHAR(255),
    failure_reason VARCHAR(500),
    raw_gateway_response TEXT,
    reconciled_by UUID REFERENCES users(id) ON DELETE SET NULL,
    reconciled_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_momo_tx_momo_id ON momo_transactions(momo_transaction_id);
CREATE INDEX idx_momo_tx_ext_ref ON momo_transactions(external_reference);
CREATE INDEX idx_momo_tx_motari ON momo_transactions(motari_id);
CREATE INDEX idx_momo_tx_status ON momo_transactions(status);
CREATE INDEX idx_momo_tx_created ON momo_transactions(created_at);

-- 4. Seed Default Rwandan Motari Subscription Plans
INSERT INTO subscription_plans (id, name, duration_days, price_rwf, description, is_popular)
VALUES 
('DAILY_PASS', 'Daily Pass', 1, 1000.00, 'Ideal for part-time or weekend Motaris (24 hours access)', FALSE),
('WEEKLY_FLEX', 'Weekly Flex', 7, 5000.00, 'Flexible week-to-week access for agile drivers (7 days)', FALSE),
('MONTHLY_STANDARD', 'Monthly Standard', 30, 15000.00, 'Most popular plan for full-time professional Motaris (30 days + 48h grace)', TRUE),
('QUARTERLY_PRO', 'Quarterly Pro', 90, 40000.00, 'Prepaid 3-month cooperative fleet tier (save 5,000 RWF)', FALSE)
ON CONFLICT (id) DO NOTHING;
