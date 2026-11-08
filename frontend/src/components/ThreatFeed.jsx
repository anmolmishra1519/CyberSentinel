import ThreatBadge from './ThreatBadge'
import ConfidenceBadge from './ConfidenceBadge'
import EmptyState from './EmptyState'
import { ShieldOff } from 'lucide-react'

export default function ThreatFeed({ threats, onSelect }) {
  if (!threats || threats.length === 0) {
    return <EmptyState title="No active threats" message="The live feed will populate as soon as a threat is detected." />
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {threats.map((t) => (
        <button
          key={t.id}
          onClick={() => onSelect?.(t)}
          style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            gap: 12, padding: '12px 14px', borderRadius: 10, border: '1px solid var(--border)',
            background: 'var(--surface-alt)', textAlign: 'left', width: '100%',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <ShieldOff size={16} color="var(--critical-fg)" />
            <div>
              <div style={{ fontSize: 13.5, fontWeight: 600 }}>
                {t.threatType === 'PortScan' ? 'Port Scan' : t.threatType === 'BruteForce' ? 'Brute Force' : t.threatType}
              </div>
              <div style={{ fontSize: 11.5, color: 'var(--ink-500)' }} className="mono">
                {t.sourceIP} → {t.destinationIP}
              </div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <ConfidenceBadge confidence={t.confidence} />
            <ThreatBadge severity={t.severity} />
          </div>
        </button>
      ))}
    </div>
  )
}
