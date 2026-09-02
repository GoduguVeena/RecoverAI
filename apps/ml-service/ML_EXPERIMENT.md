# RecoverAI — ML Experiment Specification: Revenue Recovery Probability

> **Experiment Phase**: Phase 4 — Synthetic Dataset & Recovery Probability Prediction  
> **Model Objective**: Predict \(P(\text{recovered\_within\_window} \mid \text{payment\_context})\)

---

## 1. Problem Definition & Prediction Point

RecoverAI evaluates at-risk or failed payments to determine the probability that a payment recovery intervention will succeed within a 72-hour recovery window. 

The prediction point occurs **immediately after payment failure detection** and **before any recovery action is initiated**.

---

## 2. Dataset Assumptions & Generation Process

- **Dataset Type**: Reproducible synthetic dataset (generated via `python training/generate_data.py --rows 10000 --seed 42`).
- **Rows**: 10,000 transaction records spanning 20 merchants and 1,000 customers.
- **Target Variable**: `recovered_within_window` (1 = recovered within 72 hours, 0 = unrecovered).
- **Target Sampling**: Generated using a log-odds logistic model conditioned on failure type, payment method, customer success history, retry count, transaction amount, and customer segment, combined with Gaussian stochastic noise to emulate real-world variance.

---

## 3. Feature Definitions (18 Input Features)

| Feature Name | Type | Description |
| :--- | :--- | :--- |
| `merchant_id` | Categorical | Unique identifier of the merchant |
| `customer_id` | Categorical | Unique identifier of the customer |
| `amount` | Numerical | Transaction amount (INR) |
| `currency` | Categorical | Currency code (`INR`) |
| `payment_method` | Categorical | `upi`, `card`, `netbanking`, `wallet`, `emi` |
| `failure_type` | Categorical | Failure classification (`TRANSIENT_NETWORK_TIMEOUT`, `INSUFFICIENT_FUNDS`, etc.) |
| `retry_count` | Numerical | Number of previous payment retries attempted (0 to 3) |
| `customer_total_transactions` | Numerical | Total historical transactions by customer |
| `customer_successful_transactions` | Numerical | Successful historical transactions by customer |
| `customer_failed_transactions` | Numerical | Failed historical transactions by customer |
| `customer_success_rate` | Numerical | Ratio of successful transactions (0.0 to 1.0) |
| `customer_total_spend` | Numerical | Total cumulative spend by customer |
| `days_since_last_success` | Numerical | Days elapsed since customer's last successful transaction |
| `checkout_duration_seconds` | Numerical | Time spent in checkout before attempt |
| `hour_of_day` | Numerical | Hour when transaction failure occurred (0–23) |
| `day_of_week` | Numerical | Day of week when transaction failure occurred (0–6) |
| `merchant_category` | Categorical | Merchant domain (`ecommerce`, `saas`, `education`, etc.) |
| `customer_segment` | Categorical | Customer category (`vip`, `regular`, `new`, `at_risk`) |

---

## 4. Data Leakage Prevention Rules

The following post-decision variables are **strictly forbidden** during model training and inference:
- `recovery_attempt_success`
- `recovered_amount`
- `final_payment_status`
- `recovery_action_taken`
- `recovery_timestamp`

---

## 5. Data Split & Preprocessing

- **Splits**: 70% Train (7,000 samples), 15% Validation (1,500 samples), 15% Test (1,500 samples).
- **Stratification**: Maintained class ratio across all splits using `stratify=y`.
- **Pipeline**: Categorical OneHotEncoding (`handle_unknown='ignore'`) + Numerical StandardScaler.
- **Leakage Boundary**: Preprocessor fitted **exclusively on the training split**.

---

## 6. Model Evaluation & Selection

Models evaluated on the held-out 15% test set:

1. **Logistic Regression** (Interpretable baseline)
2. **Random Forest Classifier** (100 trees, `max_depth=10`)

Artifacts stored in `experiments/`:
- `metrics.json`
- `model_comparison.json`
- `feature_importance.json`
- `models/recovery_model.joblib`

---

## 7. Limitations

1. **Synthetic Nature**: Data is generated synthetically to establish benchmark performance before live merchant integration.
2. **No Deep Learning**: Tabular tree and linear models are prioritized for explainability and low inference latency.
