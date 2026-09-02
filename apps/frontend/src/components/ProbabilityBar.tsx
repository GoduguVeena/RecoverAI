import { toProbabilityPct, formatProbability } from '../utils/format';

interface Props {
  value: string | number | null | undefined;
}

export default function ProbabilityBar({ value }: Props) {
  const pct = toProbabilityPct(value);

  if (pct === null) {
    return <span style={{ color: 'var(--text-muted)', fontSize: '12px' }}>Not analyzed</span>;
  }

  // Neutral color that doesn't imply business rules
  const barColor = 'var(--brand)';

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: '120px' }}>
      <div style={{
        flex: 1,
        height: '6px',
        borderRadius: '3px',
        backgroundColor: 'var(--bg-input)',
        overflow: 'hidden',
      }}>
        <div style={{
          width: `${pct}%`,
          height: '100%',
          borderRadius: '3px',
          backgroundColor: barColor,
          transition: 'width 0.3s ease',
        }} />
      </div>
      <span style={{
        fontSize: '12px',
        fontWeight: 600,
        fontFamily: 'var(--font-mono)',
        color: 'var(--text-primary)',
        minWidth: '44px',
        textAlign: 'right',
      }}>
        {formatProbability(value)}
      </span>
    </div>
  );
}
