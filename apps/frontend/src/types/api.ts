// ─── Wrapper ──────────────────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  requestId?: string;
  timestamp?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// ─── Enums ────────────────────────────────────────────────────────────────────

export type RecoveryCaseStatus =
  | 'OPEN'
  | 'ANALYZING'
  | 'ACTION_PENDING'
  | 'AWAITING_APPROVAL'
  | 'RECOVERED'
  | 'ESCALATED'
  | 'STOPPED'
  | 'FAILED';

export type PolicyDecisionOutcome =
  | 'ACTION_ALLOWED'
  | 'HUMAN_APPROVAL_REQUIRED'
  | 'ACTION_BLOCKED';

export type RecoveryActionType =
  | 'RETRY'
  | 'PAYMENT_LINK'
  | 'NOTIFICATION'
  | 'ESCALATE'
  | 'STOP';

export type PaymentStatus =
  | 'PENDING'
  | 'CAPTURED'
  | 'FAILED'
  | 'REFUNDED';

// ─── Recovery Case ────────────────────────────────────────────────────────────

export interface RecoveryCaseResponse {
  id: string;
  paymentId: string;
  merchantId: string;
  status: RecoveryCaseStatus;
  /** Decimal string, e.g. "0.7423". Null until ML analysis has run. */
  recoveryProbability: string | null;
  diagnosis: string | null;
  /** Decimal string (INR amount). Null until analysis has run. */
  expectedRecoveryValue: string | null;
  recommendedAction: RecoveryActionType | null;
  currentAction: RecoveryActionType | null;
  createdAt: string; // ISO-8601
  resolvedAt: string | null;
}

// ─── Policy ───────────────────────────────────────────────────────────────────

export interface PolicyCheckDetails {
  recoveryCaseEligible: boolean;
  autoRecoveryEnabled: boolean;
  retryLimitPassed: boolean;
  probabilityThresholdPassed: boolean;
  permanentFailureCheckPassed: boolean;
  cooldownPassed: boolean;
  amountWithinAutomaticLimit: boolean;
  humanApprovalThresholdCheckPassed: boolean;
  actionSupported: boolean;
}

export interface PolicyEvaluationResult {
  decision: PolicyDecisionOutcome;
  proposedAction: RecoveryActionType | null;
  reason: string;
  recoveryProbability: string | null;
  paymentAmount: string | null;
  checks: PolicyCheckDetails;
}

// ─── Agent ────────────────────────────────────────────────────────────────────

export interface AgentDecisionResponse {
  id: string;
  recoveryCaseId: string;
  modelVersion: string | null;
  /** Decimal string, e.g. "0.7423" */
  modelProbability: string | null;
  diagnosis: string | null;
  /** Raw string from agent output — may be comma-separated or JSON-ish */
  candidateActions: string | null;
  selectedAction: RecoveryActionType | null;
  reasoningSummary: string | null;
  /** Raw string from agent output */
  policyChecks: string | null;
  createdAt: string;
}

// ─── Analysis ─────────────────────────────────────────────────────────────────

export interface RecoveryAnalysisResponse {
  caseId: string;
  agentDecision: AgentDecisionResponse;
  policyDecision: PolicyEvaluationResult;
}

// ─── Execution ────────────────────────────────────────────────────────────────

export interface ExecutionResult {
  executionId: string;
  action: RecoveryActionType;
  status: string;
  message: string;
  simulated: boolean;
  simulatedPayload: string | null;
}

export interface RecoveryExecutionResponse {
  caseId: string;
  attemptId: string | null;
  executed: boolean;
  policyDecision: PolicyEvaluationResult;
  executionResult: ExecutionResult | null;
}

// ─── Payment ──────────────────────────────────────────────────────────────────

export interface PaymentResponse {
  id: string;
  merchantId: string;
  customerId: string;
  razorpayPaymentId: string | null;
  razorpayOrderId: string | null;
  /** Decimal string, e.g. "1500.00" */
  amount: string;
  currency: string;
  status: PaymentStatus;
  method: string | null;
  failureCode: string | null;
  failureReason: string | null;
  retryCount: number;
  createdAt: string;
}

// ─── Customer ─────────────────────────────────────────────────────────────────

export interface CustomerResponse {
  id: string;
  merchantId: string;
  externalCustomerId: string | null;
  name: string | null;
  email: string | null;
  phone: string | null;
  totalTransactions: number;
  successfulTransactions: number;
  failedTransactions: number;
  /** Decimal string */
  totalSpend: string | null;
  createdAt: string;
}

// ─── Health ───────────────────────────────────────────────────────────────────

export interface HealthResponse {
  status: 'UP' | 'DOWN' | string;
  service?: string;
}

// ─── Client-side error ────────────────────────────────────────────────────────

export interface ApiError {
  status: number;
  message: string;
  requestId?: string;
}
