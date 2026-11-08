/**
 * Signature visual element: a thin circular gauge showing 0-100 risk,
 * colored by severity band. Reused on stat cards and the threat detail view.
 */
const COLOR_BY_BAND = {
  SAFE: 'var(--safe-fg)',
  LOW: 'var(--low-fg)',
  MEDIUM: 'var(--medium-fg)',
  HIGH: 'var(--high-fg)',
  CRITICAL: 'var(--critical-fg)',
}

function bandFor(risk) {
  if (risk >= 85) return 'CRITICAL'
  if (risk >= 70) return 'HIGH'
  if (risk >= 40) return 'MEDIUM'
  if (risk >= 15) return 'LOW'
  return 'SAFE'
}

export default function RiskScore({ value = 0, size = 56, label }) {
  const radius = (size - 8) / 2
  const circumference = 2 * Math.PI * radius
  const clamped = Math.max(0, Math.min(100, value))
  const offset = circumference - (clamped / 100) * circumference
  const color = COLOR_BY_BAND[bandFor(clamped)]

  return (
    <div className="risk-gauge">
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <circle
          cx={size / 2} cy={size / 2} r={radius}
          fill="none" stroke="var(--border)" strokeWidth="5"
        />
        <circle
          cx={size / 2} cy={size / 2} r={radius}
          fill="none" stroke={color} strokeWidth="5"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
          style={{ transition: 'stroke-dashoffset 0.5s ease' }}
        />
      </svg>
      <div>
        <div className="risk-gauge-value" style={{ color }}>{Math.round(clamped)}</div>
        {label && <div style={{ fontSize: 11, color: 'var(--ink-500)' }}>{label}</div>}
      </div>
    </div>
  )
}
