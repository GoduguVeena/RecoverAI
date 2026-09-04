import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useServiceHealth } from '../hooks/useServiceHealth';
import { fetchRecoveryCase, analyzeRecoveryCase, executeRecoveryCase } from '../api/cases';
import { fetchPayment } from '../api/payments';
import { fetchCustomer } from '../api/customers';
import type {
  RecoveryCaseResponse,
  PaymentResponse,
  CustomerResponse,
  RecoveryAnalysisResponse,
  RecoveryExecutionResponse,
  PolicyCheckDetails,
} from '../types/api';
import Nav from '../components/Nav';
import CaseStatusBadge from '../components/CaseStatusBadge';
import PolicyDecisionBadge from '../components/PolicyDecisionBadge';
import ProbabilityBar from '../components/ProbabilityBar';
import {
  shortId, formatCurrency, formatDateTime, formatProbability,
  actionLabel, formatRelativeTime,
} from '../utils/format';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ActivityEntry {
  ts: Date;
  text: string;
  type: 'info' | 'success' | 'warning' | 'error';
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function Section({ title, tag, children }: { title: string; tag?: string; children: React.ReactNode }) {
  return (
    <div style={{
      backgroundColor: 'var(--bg-surface)',
      border: '1px solid var(--border-subtle)',
      borderRadius: 'var(--radius-lg)',
      overflow: 'hidden',
    }}>
      <div style={{
        padding: '12px 20px',
        borderBottom: '1px solid var(--border-subtle)',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
      }}>
        <h3 style={{ fontSize: '12px', fontWeight: 700, letterSpacing: '0.07em', textTransform: 'uppercase', color: 'var(--text-muted)', margin: 0 }}>
          {title}
        </h3>
        {tag && (
          <span style={{ fontSize: '10px', fontWeight: 700, padding: '1px 6px', borderRadius: '3px', backgroundColor: 'rgba(56,189,248,0.1)', color: 'var(--brand)', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
            {tag}
          </span>
        )}
      </div>
      <div style={{ padding: '16px 20px' }}>{children}</div>
    </div>
  );
}

function Field({ label, value, mono }: { label: string; value: React.ReactNode; mono?: boolean }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', minWidth: 0 }}>
      <span style={{ fontSize: '10px', fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.06em', textTransform: 'uppercase' }}>{label}</span>
      <span style={{
        fontSize: '13px',
        color: value ? 'var(--text-primary)' : 'var(--text-muted)',
        fontFamily: mono ? 'var(--font-mono)' : undefined,
        wordBreak: 'break-all',
      }}>
        {value ?? '—'}
      </span>
    </div>
  );
}

function FieldGrid({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '16px' }}>
      {children}
    </div>
  );
}

function PolicyChecks({ checks }: { checks: PolicyCheckDetails }) {
  const items: Array<[string, boolean]> = [
    ['Recovery case eligible', checks.recoveryCaseEligible],
    ['Auto recovery enabled', checks.autoRecoveryEnabled],
    ['Retry limit not exceeded', checks.retryLimitPassed],
    ['Probability threshold passed', checks.probabilityThresholdPassed],
    ['Permanent failure check passed', checks.permanentFailureCheckPassed],
    ['Cooldown period respected', checks.cooldownPassed],
    ['Amount within automatic limit', checks.amountWithinAutomaticLimit],
    ['Human approval threshold passed', checks.humanApprovalThresholdCheckPassed],
    ['Action type supported', checks.actionSupported],
  ];
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '6px', marginTop: '12px' }}>
      {items.map(([label, passed]) => (
        <div key={label} style={{
          display: 'flex', alignItems: 'center', gap: '8px',
          padding: '6px 10px',
          borderRadius: 'var(--radius-sm)',
          backgroundColor: passed ? 'rgba(74,222,128,0.06)' : 'rgba(248,113,113,0.06)',
          border: `1px solid ${passed ? 'rgba(74,222,128,0.15)' : 'rgba(248,113,113,0.15)'}`,
        }}>
          <span style={{ fontSize: '12px', color: passed ? 'var(--policy-allowed)' : 'var(--policy-blocked)', flexShrink: 0 }}>
            {passed ? '✓' : '✕'}
          </span>
          <span style={{ fontSize: '12px', color: passed ? 'var(--text-primary)' : 'var(--text-muted)' }}>{label}</span>
        </div>
      ))}
    </div>
  );
}

function ActivityLog({ entries }: { entries: ActivityEntry[] }) {
  const colors = { info: 'var(--text-muted)', success: 'var(--status-recovered)', warning: 'var(--status-awaiting)', error: 'var(--status-failed)' };
  const dots = { info: 'var(--border-subtle)', success: 'var(--policy-allowed)', warning: 'var(--policy-approval)', error: 'var(--policy-blocked)' };
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0' }}>
      {entries.map((e, i) => (
        <div key={i} style={{ display: 'flex', gap: '12px', alignItems: 'flex-start', position: 'relative' }}>
          {/* Line */}
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flexShrink: 0 }}>
            <div style={{ width: 8, height: 8, borderRadius: '50%', backgroundColor: dots[e.type], marginTop: 4 }} />
            {i < entries.length - 1 && (
              <div style={{ width: 1, flex: 1, backgroundColor: 'var(--border)', minHeight: '20px' }} />
            )}
          </div>
          <div style={{ paddingBottom: '14px', minWidth: 0 }}>
            <div style={{ fontSize: '12px', color: colors[e.type] }}>{e.text}</div>
            <div style={{ fontSize: '10px', color: 'var(--text-muted)', marginTop: 1 }}>
              {e.ts.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function CaseDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const health = useServiceHealth();

  const [caseData, setCaseData] = useState<RecoveryCaseResponse | null>(null);
  const [paymentData, setPaymentData] = useState<PaymentResponse | null>(null);
  const [customerData, setCustomerData] = useState<CustomerResponse | null>(null);
  const [analysisResult, setAnalysisResult] = useState<RecoveryAnalysisResponse | null>(null);
  const [executionResult, setExecutionResult] = useState<RecoveryExecutionResponse | null>(null);

  const [caseLoading, setCaseLoading] = useState(true);
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [customerLoading, setCustomerLoading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [executing, setExecuting] = useState(false);

  const [caseError, setCaseError] = useState<string | null>(null);
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [analyzeError, setAnalyzeError] = useState<string | null>(null);
  const [executeError, setExecuteError] = useState<string | null>(null);

  const [activity, setActivity] = useState<ActivityEntry[]>([]);

  const addActivity = useCallback((text: string, type: ActivityEntry['type'] = 'info') => {
    setActivity((prev) => [...prev, { ts: new Date(), text, type }]);
  }, []);

  // ── Load case ────────────────────────────────────────────────────────────────
  useEffect(() => {
    if (!id) return;
    setCaseLoading(true);
    setCaseError(null);
    fetchRecoveryCase(id)
      .then((data) => {
        setCaseData(data);
        addActivity(`Case ${shortId(data.id)} loaded`, 'info');
        // Load payment
        setPaymentLoading(true);
        return fetchPayment(data.paymentId);
      })
      .then((payment) => {
        setPaymentData(payment);
        addActivity('Payment context loaded', 'info');
        setPaymentLoading(false);
        // Load customer
        setCustomerLoading(true);
        return fetchCustomer(payment.customerId);
      })
      .then((customer) => {
        setCustomerData(customer);
        addActivity('Customer context loaded', 'info');
        setCustomerLoading(false);
      })
      .catch((err: unknown) => {
        const msg = err instanceof Error ? err.message : 'Failed to load';
        if (!caseData) { setCaseError(msg); setCaseLoading(false); }
        else if (!paymentData) { setPaymentError(msg); setPaymentLoading(false); setCustomerLoading(false); }
        else { setPaymentError(msg); setCustomerLoading(false); }
      })
      .finally(() => setCaseLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  // ── Analyze ──────────────────────────────────────────────────────────────────
  const handleAnalyze = useCallback(async () => {
    if (!id || analyzing) return;
    setAnalyzing(true);
    setAnalyzeError(null);
    setAnalysisResult(null);
    setExecutionResult(null);
    addActivity('Analysis requested', 'info');
    try {
      const result = await analyzeRecoveryCase(id);
      setAnalysisResult(result);
      // Refresh case status
      const refreshed = await fetchRecoveryCase(id);
      setCaseData(refreshed);
      addActivity(`AI recommendation: ${actionLabel(result.agentDecision.selectedAction)}`, 'success');
      addActivity(`Policy decision: ${result.policyDecision.decision}`,
        result.policyDecision.decision === 'ACTION_ALLOWED' ? 'success'
        : result.policyDecision.decision === 'ACTION_BLOCKED' ? 'error' : 'warning');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Analysis failed';
      setAnalyzeError(msg);
      addActivity(`Analysis failed: ${msg}`, 'error');
    } finally {
      setAnalyzing(false);
    }
  }, [id, analyzing, addActivity]);

  // ── Execute ──────────────────────────────────────────────────────────────────
  const handleExecute = useCallback(async () => {
    if (!id || executing) return;
    setExecuting(true);
    setExecuteError(null);
    addActivity('Execution requested (dry-run)', 'warning');
    try {
      const result = await executeRecoveryCase(id);
      setExecutionResult(result);
      // Refresh case status
      const refreshed = await fetchRecoveryCase(id);
      setCaseData(refreshed);
      if (result.executed) {
        addActivity('Dry-run execution completed', 'success');
      } else {
        addActivity(`Execution not performed: ${result.policyDecision.decision}`, 'warning');
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Execution failed';
      setExecuteError(msg);
      addActivity(`Execution error: ${msg}`, 'error');
    } finally {
      setExecuting(false);
    }
  }, [id, executing, addActivity]);

  // ── Derived state ─────────────────────────────────────────────────────────────
  const policyDecision = analysisResult?.policyDecision;
  const agentDecision = analysisResult?.agentDecision;
  const canExecute = policyDecision?.decision === 'ACTION_ALLOWED' && !executionResult;

  // ─── Render ──────────────────────────────────────────────────────────────────

  if (caseLoading) {
    return (
      <div style={{ minHeight: '100vh', backgroundColor: 'var(--bg-base)' }}>
        <Nav backend={health.backend} ml={health.ml} />
        <div style={{ padding: 'var(--space-8)', textAlign: 'center', color: 'var(--text-muted)' }}>
          Loading case...
        </div>
      </div>
    );
  }

  if (caseError || !caseData) {
    return (
      <div style={{ minHeight: '100vh', backgroundColor: 'var(--bg-base)' }}>
        <Nav backend={health.backend} ml={health.ml} />
        <div style={{ padding: 'var(--space-8)', maxWidth: 600, margin: '0 auto', textAlign: 'center' }}>
          <div style={{ color: 'var(--status-failed)', marginBottom: 'var(--space-4)' }}>
            ⚠ {caseError ?? 'Case not found'}
          </div>
          <button onClick={() => navigate('/')} style={btnStyle('secondary')}>← Back to Dashboard</button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', backgroundColor: 'var(--bg-base)' }}>
      <Nav backend={health.backend} ml={health.ml} />

      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: 'var(--space-6)', display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>

        {/* ── Back + Header ── */}
        <div>
          <button onClick={() => navigate('/')} style={{ ...btnStyle('ghost'), marginBottom: 'var(--space-4)' }}>
            ← Recovery Cases
          </button>

          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: 'var(--space-4)' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
                <h1 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)', margin: 0, fontFamily: 'var(--font-mono)' }}>
                  CASE · {shortId(caseData.id)}
                </h1>
                <CaseStatusBadge status={caseData.status} />
              </div>
              <div style={{ marginTop: '6px', display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
                <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                  Created {formatRelativeTime(caseData.createdAt)} · {formatDateTime(caseData.createdAt)}
                </span>
                <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                  Payment: {shortId(caseData.paymentId)}
                </span>
                <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                  Merchant: {shortId(caseData.merchantId)}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* ── Safety Banner ── */}
        <div style={{
          backgroundColor: 'rgba(251,146,60,0.06)',
          border: '1px solid rgba(251,146,60,0.25)',
          borderRadius: 'var(--radius-md)',
          padding: '10px 16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '8px',
        }}>
          <span style={{ fontSize: '12px', color: '#fb923c', fontWeight: 600 }}>
            🛡 AI recommends · Policy Engine authorizes · Execution Adapter executes
          </span>
          <span style={{
            fontSize: '11px', fontWeight: 700, letterSpacing: '0.06em',
            backgroundColor: 'rgba(251,146,60,0.15)', color: '#fb923c',
            padding: '2px 8px', borderRadius: '3px', border: '1px solid rgba(251,146,60,0.3)',
          }}>
            ALL EXECUTIONS ARE DRY RUN SIMULATIONS
          </span>
        </div>

        {/* ── Two-column layout ── */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 'var(--space-5)', alignItems: 'start' }}>

          {/* Left column */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>

            {/* ── Payment Context ── */}
            <Section title="Payment Context">
              {paymentLoading ? (
                <div style={{ color: 'var(--text-muted)', fontSize: '13px' }}>Loading payment...</div>
              ) : paymentError ? (
                <div style={{ color: 'var(--status-failed)', fontSize: '13px' }}>⚠ {paymentError}</div>
              ) : paymentData ? (
                <FieldGrid>
                  <Field label="Amount" value={formatCurrency(paymentData.amount, paymentData.currency)} />
                  <Field label="Currency" value={paymentData.currency} />
                  <Field label="Payment Status" value={paymentData.status} />
                  <Field label="Method" value={paymentData.method ?? null} />
                  <Field label="Failure Code" value={paymentData.failureCode ?? null} />
                  <Field label="Failure Reason" value={paymentData.failureReason ?? null} />
                  <Field label="Retry Count" value={String(paymentData.retryCount)} />
                  <Field label="Razorpay Payment ID" value={paymentData.razorpayPaymentId ?? null} mono />
                  <Field label="Razorpay Order ID" value={paymentData.razorpayOrderId ?? null} mono />
                  <Field label="Created" value={formatDateTime(paymentData.createdAt)} />
                </FieldGrid>
              ) : null}
            </Section>

            {/* ── Customer Context ── */}
            <Section title="Customer Context">
              {customerLoading ? (
                <div style={{ color: 'var(--text-muted)', fontSize: '13px' }}>Loading customer...</div>
              ) : paymentError && !customerData ? (
                <div style={{ color: 'var(--text-muted)', fontSize: '13px' }}>Customer unavailable</div>
              ) : customerData ? (
                <FieldGrid>
                  <Field label="Name" value={customerData.name ?? null} />
                  <Field label="Email" value={customerData.email ?? null} />
                  <Field label="Phone" value={customerData.phone ?? null} />
                  <Field label="Total Transactions" value={String(customerData.totalTransactions)} />
                  <Field label="Successful" value={String(customerData.successfulTransactions)} />
                  <Field label="Failed" value={String(customerData.failedTransactions)} />
                  <Field label="Success Rate" value={
                    customerData.totalTransactions > 0
                      ? `${((customerData.successfulTransactions / customerData.totalTransactions) * 100).toFixed(1)}%`
                      : '—'
                  } />
                  <Field label="Total Spend" value={customerData.totalSpend ? formatCurrency(customerData.totalSpend) : null} />
                </FieldGrid>
              ) : null}
            </Section>

            {/* ── Recovery Intelligence ── */}
            <Section title="Recovery Intelligence" tag="ML + AI + Policy">
              {!analysisResult && !analyzing && (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: '16px' }}>
                  <div style={{ color: 'var(--text-muted)', fontSize: '13px' }}>
                    This case has not been analyzed yet. Click below to run ML prediction, AI diagnosis, and Policy evaluation.
                  </div>
                  {analyzeError && (
                    <div style={{
                      backgroundColor: 'rgba(248,113,113,0.08)',
                      border: '1px solid rgba(248,113,113,0.25)',
                      borderRadius: 'var(--radius-sm)',
                      padding: '10px 14px',
                      color: 'var(--status-failed)',
                      fontSize: '13px',
                    }}>
                      ⚠ Analysis failed: {analyzeError}
                    </div>
                  )}
                  <button onClick={handleAnalyze} style={btnStyle('primary')}>
                    Analyze Recovery
                  </button>
                </div>
              )}

              {analyzing && (
                <div style={{ color: 'var(--text-muted)', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <span style={{ animation: 'spin 1s linear infinite', display: 'inline-block' }}>↻</span>
                  Analyzing recovery opportunity...
                </div>
              )}

              {analysisResult && !analyzing && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>

                  {/* A. ML Prediction */}
                  <div>
                    <div style={{ fontSize: '10px', fontWeight: 700, color: 'var(--text-muted)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: '10px' }}>
                      ML Prediction
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <span style={{ fontSize: '11px', color: 'var(--text-muted)', width: '130px', flexShrink: 0 }}>Recovery Probability</span>
                        <div style={{ flex: 1 }}>
                          <ProbabilityBar value={agentDecision?.modelProbability} />
                        </div>
                      </div>
                      <div style={{ display: 'flex', gap: '24px' }}>
                        <Field label="Model" value={agentDecision?.modelVersion ?? null} mono />
                        <Field label="Probability" value={formatProbability(agentDecision?.modelProbability)} />
                      </div>
                    </div>
                  </div>

                  <div style={{ height: 1, backgroundColor: 'var(--border-subtle)' }} />

                  {/* B. AI Agent */}
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
                      <div style={{ fontSize: '10px', fontWeight: 700, color: 'var(--text-muted)', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                        AI Recommendation
                      </div>
                      <span style={{
                        fontSize: '10px', fontWeight: 700,
                        backgroundColor: 'rgba(250,204,21,0.1)', color: '#facc15',
                        padding: '1px 6px', borderRadius: '3px', letterSpacing: '0.05em',
                      }}>
                        NOT AUTHORIZATION
                      </span>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                      {agentDecision?.diagnosis && (
                        <div>
                          <div style={{ fontSize: '10px', color: 'var(--text-muted)', fontWeight: 600, letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: '4px' }}>Diagnosis</div>
                          <div style={{ fontSize: '13px', color: 'var(--text-primary)', lineHeight: 1.5 }}>{agentDecision.diagnosis}</div>
                        </div>
                      )}
                      {agentDecision?.candidateActions && (
                        <div>
                          <div style={{ fontSize: '10px', color: 'var(--text-muted)', fontWeight: 600, letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: '4px' }}>Candidate Actions</div>
                          <div style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: 1.5 }}>{agentDecision.candidateActions}</div>
                        </div>
                      )}
                      {agentDecision?.selectedAction && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <span style={{ fontSize: '10px', color: 'var(--text-muted)', fontWeight: 600, letterSpacing: '0.06em', textTransform: 'uppercase' }}>Selected Action</span>
                          <span style={{
                            fontSize: '12px', fontWeight: 700,
                            backgroundColor: 'rgba(56,189,248,0.1)', color: 'var(--brand)',
                            padding: '3px 10px', borderRadius: '4px', letterSpacing: '0.04em', textTransform: 'uppercase',
                          }}>
                            {actionLabel(agentDecision.selectedAction)}
                          </span>
                        </div>
                      )}
                      {agentDecision?.reasoningSummary && (
                        <div>
                          <div style={{ fontSize: '10px', color: 'var(--text-muted)', fontWeight: 600, letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: '4px' }}>Reasoning Summary</div>
                          <div style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: 1.5, fontStyle: 'italic' }}>{agentDecision.reasoningSummary}</div>
                        </div>
                      )}
                    </div>
                  </div>

                  <div style={{ height: 1, backgroundColor: 'var(--border-subtle)' }} />

                  {/* C. Policy Engine */}
                  <div>
                    <div style={{ fontSize: '10px', fontWeight: 700, color: 'var(--text-muted)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: '12px' }}>
                      Policy Engine Decision
                    </div>
                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: '16px', flexWrap: 'wrap', marginBottom: '12px' }}>
                      <PolicyDecisionBadge decision={policyDecision!.decision} size="lg" />
                      <div style={{ flex: 1, minWidth: '200px' }}>
                        <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginBottom: '3px', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Reason</div>
                        <div style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: 1.5 }}>{policyDecision?.reason}</div>
                      </div>
                    </div>
                    {policyDecision?.checks && <PolicyChecks checks={policyDecision.checks} />}
                  </div>

                  {/* Re-analyze option */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <button onClick={handleAnalyze} style={btnStyle('ghost')} disabled={analyzing}>
                      ↻ Re-analyze
                    </button>
                    <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                      Analyzed {formatRelativeTime(agentDecision?.createdAt)}
                    </span>
                  </div>
                </div>
              )}
            </Section>

            {/* ── Execution ── */}
            {analysisResult && !analyzing && (
              <Section title="Recovery Execution">
                {/* Execution gate display */}
                {policyDecision?.decision === 'ACTION_BLOCKED' && (
                  <div style={{
                    backgroundColor: 'rgba(248,113,113,0.08)',
                    border: '1px solid rgba(248,113,113,0.2)',
                    borderRadius: 'var(--radius-md)',
                    padding: '14px 16px',
                    display: 'flex', alignItems: 'center', gap: '10px',
                  }}>
                    <span style={{ fontSize: '18px' }}>✕</span>
                    <div>
                      <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--policy-blocked)' }}>Execution blocked by Policy Engine</div>
                      <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '3px' }}>{policyDecision.reason}</div>
                    </div>
                  </div>
                )}

                {policyDecision?.decision === 'HUMAN_APPROVAL_REQUIRED' && (
                  <div style={{
                    backgroundColor: 'rgba(250,204,21,0.06)',
                    border: '1px solid rgba(250,204,21,0.25)',
                    borderRadius: 'var(--radius-md)',
                    padding: '14px 16px',
                    display: 'flex', alignItems: 'center', gap: '10px',
                  }}>
                    <span style={{ fontSize: '18px' }}>⏸</span>
                    <div>
                      <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--policy-approval)' }}>Human approval required — automated execution unavailable</div>
                      <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '3px' }}>Manual review and approval through an authorized operator is required before this action can proceed.</div>
                    </div>
                  </div>
                )}

                {canExecute && !executionResult && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                    {/* Confirmation summary */}
                    <div style={{
                      backgroundColor: 'var(--bg-elevated)',
                      border: '1px solid var(--border-subtle)',
                      borderRadius: 'var(--radius-md)',
                      padding: '12px 16px',
                      display: 'grid',
                      gridTemplateColumns: 'repeat(3, 1fr)',
                      gap: '12px',
                    }}>
                      <Field label="Recommended Action" value={actionLabel(policyDecision?.proposedAction)} />
                      <Field label="Policy Decision" value={<PolicyDecisionBadge decision="ACTION_ALLOWED" />} />
                      <Field label="Execution Mode" value={
                        <span style={{ fontSize: '12px', fontWeight: 700, color: '#fb923c' }}>DRY RUN</span>
                      } />
                    </div>
                    {executeError && (
                      <div style={{ color: 'var(--status-failed)', fontSize: '13px', padding: '10px 14px', backgroundColor: 'rgba(248,113,113,0.08)', border: '1px solid rgba(248,113,113,0.25)', borderRadius: 'var(--radius-sm)' }}>
                        ⚠ Execution failed: {executeError}
                      </div>
                    )}
                    <button
                      onClick={handleExecute}
                      disabled={executing}
                      style={btnStyle(executing ? 'disabled' : 'execute')}
                    >
                      {executing
                        ? '↻ Executing simulated recovery...'
                        : '▶ Approve & Simulate Execution'}
                    </button>
                  </div>
                )}

                {/* Execution result */}
                {executionResult && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                    <div style={{
                      backgroundColor: executionResult.executed ? 'rgba(74,222,128,0.06)' : 'rgba(248,113,113,0.06)',
                      border: `1px solid ${executionResult.executed ? 'rgba(74,222,128,0.25)' : 'rgba(248,113,113,0.25)'}`,
                      borderRadius: 'var(--radius-md)',
                      padding: '14px 16px',
                    }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
                        <span style={{ fontSize: '16px' }}>{executionResult.executed ? '✓' : '✕'}</span>
                        <div>
                          <div style={{ fontSize: '13px', fontWeight: 700, color: executionResult.executed ? 'var(--policy-allowed)' : 'var(--policy-blocked)' }}>
                            {executionResult.executed ? 'Dry-Run Execution Completed' : 'Execution Not Performed'}
                          </div>
                          {executionResult.executed && (
                            <div style={{ fontSize: '11px', fontWeight: 700, color: '#fb923c', marginTop: '2px', letterSpacing: '0.06em' }}>
                              SIMULATED — NO REAL PAYMENT MADE
                            </div>
                          )}
                        </div>
                      </div>

                      {executionResult.executionResult && (
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '12px' }}>
                          <Field label="Action" value={actionLabel(executionResult.executionResult.action)} />
                          <Field label="Status" value={executionResult.executionResult.status} />
                          <Field label="Simulated" value={executionResult.executionResult.simulated ? 'YES' : 'NO'} />
                          <Field label="Execution ID" value={shortId(executionResult.executionResult.executionId)} mono />
                          {executionResult.attemptId && <Field label="Attempt ID" value={shortId(executionResult.attemptId)} mono />}
                        </div>
                      )}

                      {executionResult.executionResult?.message && (
                        <div style={{ marginTop: '12px' }}>
                          <Field label="Message" value={executionResult.executionResult.message} />
                        </div>
                      )}

                      {executionResult.executionResult?.simulatedPayload && (
                        <div style={{ marginTop: '12px' }}>
                          <div style={{ fontSize: '10px', color: 'var(--text-muted)', fontWeight: 600, letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: '6px' }}>Simulated Payload</div>
                          <pre style={{
                            backgroundColor: 'var(--bg-input)',
                            border: '1px solid var(--border-subtle)',
                            borderRadius: 'var(--radius-sm)',
                            padding: '10px 12px',
                            fontSize: '11px',
                            color: 'var(--text-secondary)',
                            fontFamily: 'var(--font-mono)',
                            overflowX: 'auto',
                            margin: 0,
                            whiteSpace: 'pre-wrap',
                            wordBreak: 'break-all',
                          }}>
                            {(() => {
                              try { return JSON.stringify(JSON.parse(executionResult.executionResult!.simulatedPayload!), null, 2); }
                              catch { return executionResult.executionResult!.simulatedPayload; }
                            })()}
                          </pre>
                        </div>
                      )}

                      <div style={{ marginTop: '12px', padding: '8px 12px', backgroundColor: 'rgba(251,146,60,0.08)', borderRadius: 'var(--radius-sm)', border: '1px solid rgba(251,146,60,0.2)' }}>
                        <span style={{ fontSize: '12px', color: '#fb923c', fontWeight: 600 }}>
                          ⚠ Payment status remains FAILED — this was a dry-run simulation. No real financial transaction was executed.
                        </span>
                      </div>
                    </div>

                    {/* Re-execute not available after completion */}
                    <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                      Execution recorded. Re-analyze to evaluate updated policy conditions.
                    </div>
                  </div>
                )}
              </Section>
            )}
          </div>

          {/* Right column — sidebar */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)', position: 'sticky', top: '72px' }}>

            {/* Case Details */}
            <Section title="Case Details">
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <Field label="Case ID" value={<span style={{ fontFamily: 'var(--font-mono)', fontSize: '11px', color: 'var(--brand)' }}>{caseData.id}</span>} />
                <Field label="Status" value={<CaseStatusBadge status={caseData.status} />} />
                {caseData.recoveryProbability && (
                  <div>
                    <div style={{ fontSize: '10px', fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: '6px' }}>Recovery Probability</div>
                    <ProbabilityBar value={caseData.recoveryProbability} />
                  </div>
                )}
                {caseData.recommendedAction && <Field label="Recommended Action" value={actionLabel(caseData.recommendedAction)} />}
                {caseData.expectedRecoveryValue && <Field label="Expected Recovery Value" value={formatCurrency(caseData.expectedRecoveryValue)} />}
                <Field label="Created" value={formatDateTime(caseData.createdAt)} />
                {caseData.resolvedAt && <Field label="Resolved" value={formatDateTime(caseData.resolvedAt)} />}
              </div>
            </Section>

            {/* Session Activity */}
            <Section title="Session Activity">
              {activity.length === 0 ? (
                <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>No activity yet.</div>
              ) : (
                <ActivityLog entries={activity} />
              )}
              <div style={{ marginTop: '12px', fontSize: '10px', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                Session-only view. Authoritative audit log is in the backend database.
              </div>
            </Section>
          </div>
        </div>
      </div>

      <style>{`
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
}

// ─── Button styles ────────────────────────────────────────────────────────────

function btnStyle(variant: 'primary' | 'execute' | 'secondary' | 'ghost' | 'disabled'): React.CSSProperties {
  const base: React.CSSProperties = {
    border: 'none',
    borderRadius: 'var(--radius-sm)',
    padding: '8px 16px',
    fontSize: '13px',
    fontWeight: 600,
    cursor: 'pointer',
    display: 'inline-flex',
    alignItems: 'center',
    gap: '6px',
    transition: 'opacity 0.15s',
    fontFamily: 'var(--font-sans)',
  };
  const variants: Record<string, React.CSSProperties> = {
    primary: { backgroundColor: 'var(--brand)', color: '#0a0f1a' },
    execute: { backgroundColor: 'var(--policy-allowed)', color: '#0a0f1a' },
    secondary: { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-primary)', border: '1px solid var(--border-subtle)' },
    ghost: { backgroundColor: 'transparent', color: 'var(--text-secondary)', padding: '4px 8px', fontSize: '12px' },
    disabled: { backgroundColor: 'var(--bg-elevated)', color: 'var(--text-muted)', cursor: 'default', opacity: 0.6 },
  };
  return { ...base, ...variants[variant] };
}
