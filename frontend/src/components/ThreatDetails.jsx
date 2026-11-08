import { X } from 'lucide-react'
import ThreatBadge from './ThreatBadge'
import ConfidenceBadge from './ConfidenceBadge'
import RiskScore from './RiskScore'
import ExplanationPanel from './ExplanationPanel'

function fmt(ts) {
  if (!ts) return '—'
  try { return new Date(ts).toLocaleString() } catch { return ts }
}

function label(type) {
  if (type === 'PortScan') return 'Port Scan'
  if (type === 'BruteForce') return 'Brute Force'
  return type
}

export default function ThreatDetails({ threat, onClose }) {
  if (!threat) return null

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <RiskScore value={threat.riskScore} size={46} />
            <div>
              <h3>{label(threat.threatType)} Detected</h3>
              <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
                <ThreatBadge severity={threat.severity} />
                <ConfidenceBadge confidence={threat.confidence} />
              </div>
            </div>
          </div>
          <button className="btn" onClick={onClose} style={{ padding: 8 }}><X size={16} /></button>
        </div>

        <div className="modal-body">
          <div className="detail-grid">
            <Detail label="Threat Type" value={label(threat.threatType)} />
            <Detail label="Status" value={threat.status} />
            <Detail label="Source IP" value={threat.sourceIP} />
            <Detail label="Destination IP" value={threat.destinationIP} />
            <Detail label="Protocol" value={threat.protocol} />
            <Detail label="Source Port" value={threat.sourcePort} />
            <Detail label="Destination Port" value={threat.destinationPort} />
            <Detail label="Flow Duration" value={`${threat.flowDuration}s`} />
            <Detail label="Packet Count" value={threat.packetCount} />
            <Detail label="Byte Count" value={threat.byteCount} />
            <Detail label="Confidence" value={`${Math.round(threat.confidence * 100)}%`} />
            <Detail label="Risk Score" value={threat.riskScore} />
            <Detail label="Severity" value={threat.severity} />
            <Detail label="Detection Time" value={fmt(threat.detectionTime)} />
          </div>

          <hr style={{ border: 'none', borderTop: '1px solid var(--border)' }} />

          <ExplanationPanel reasons={threat.reasons} recommendedAction={threat.recommendedAction} />
        </div>
      </div>
    </div>
  )
}

function Detail({ label, value }) {
  return (
    <div className="detail-item">
      <span className="detail-label">{label}</span>
      <span className="detail-value">{value ?? '—'}</span>
    </div>
  )
}
