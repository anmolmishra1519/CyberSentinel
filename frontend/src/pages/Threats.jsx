import { useEffect, useState, useCallback } from 'react'
import Topbar from '../components/Topbar'
import ThreatBadge from '../components/ThreatBadge'
import ConfidenceBadge from '../components/ConfidenceBadge'
import ThreatDetails from '../components/ThreatDetails'
import LoadingState from '../components/LoadingState'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import api from '../services/api'

const FILTERS = [
  { key: 'All', label: 'All' },
  { key: 'DDoS', label: 'DDoS/DoS', param: 'type' },
  { key: 'PortScan', label: 'Port Scan', param: 'type' },
  { key: 'BruteForce', label: 'Brute Force', param: 'type' },
  { key: 'CRITICAL', label: 'Critical', param: 'severity' },
  { key: 'HIGH', label: 'High', param: 'severity' },
  { key: 'MEDIUM', label: 'Medium', param: 'severity' },
]

const LABEL = { PortScan: 'Port Scan', BruteForce: 'Brute Force', DDoS: 'DDoS/DoS', Normal: 'Normal' }

function fmtTime(ts) {
  if (!ts) return '—'
  try { return new Date(ts).toLocaleTimeString() } catch { return ts }
}

export default function Threats() {
  const [active, setActive] = useState('All')
  const [threats, setThreats] = useState([]);
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selected, setSelected] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const filter = FILTERS.find((f) => f.key === active)
      const params = {}
      if (filter?.param === 'type') params.type = filter.key
      if (filter?.param === 'severity') params.severity = filter.key
      const data = await api.getThreats(params)
      setThreats(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [active])

  useEffect(() => { load() }, [load])

  return (
    <>
      <Topbar title="Threats" subtitle="All detections produced by the ML pipeline" />
      <div className="page-content">
        <div className="filter-row">
          {FILTERS.map((f) => (
            <button
              key={f.key}
              className={`filter-chip${active === f.key ? ' active' : ''}`}
              onClick={() => setActive(f.key)}
            >
              {f.label}
            </button>
          ))}
        </div>

        <div className="card card-padded">
          {loading && <LoadingState message="Loading threats…" />}
          {!loading && error && <ErrorState message={error} onRetry={load} />}
          {!loading && !error && threats.length === 0 && (
            <EmptyState title="No threats found" message="Try a different filter, or run the Attack Simulator." />
          )}
          {!loading && !error && threats.length > 0 && (
            <table>
              <thead>
                <tr>
                  <th>Threat</th><th>Source IP</th><th>Destination IP</th>
                  <th>Confidence</th><th>Risk</th><th>Severity</th><th>Time</th><th>Status</th>
                </tr>
              </thead>
              <tbody>
                {threats.map((t) => (
                  <tr key={t.id} onClick={() => setSelected(t)}>
                    <td>{LABEL[t.threatType] || t.threatType}</td>
                    <td className="mono">{t.sourceIP}</td>
                    <td className="mono">{t.destinationIP}</td>
                    <td><ConfidenceBadge confidence={t.confidence} /></td>
                    <td className="mono">{t.riskScore}</td>
                    <td><ThreatBadge severity={t.severity} /></td>
                    <td className="mono" style={{ fontSize: 12 }}>{fmtTime(t.detectionTime)}</td>
                    <td><span className="badge badge-outline">{t.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {selected && <ThreatDetails threat={selected} onClose={() => setSelected(null)} />}
    </>
  )
}
