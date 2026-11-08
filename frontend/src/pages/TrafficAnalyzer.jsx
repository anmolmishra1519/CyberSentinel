import { useState } from 'react'
import Topbar from '../components/Topbar'
import FileUploader from '../components/FileUploader'
import ThreatBadge from '../components/ThreatBadge'
import ConfidenceBadge from '../components/ConfidenceBadge'
import ThreatDetails from '../components/ThreatDetails'
import LoadingState from '../components/LoadingState'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import api from '../services/api'
import { PlayCircle } from 'lucide-react'

const CLASS_ORDER = ['Normal', 'DDoS', 'PortScan', 'BruteForce']
const CLASS_LABEL = { Normal: 'Normal', DDoS: 'DDoS/DoS', PortScan: 'Port Scan', BruteForce: 'Brute Force' }

export default function TrafficAnalyzer() {
  const [file, setFile] = useState(null)
  const [validation, setValidation] = useState(null)
  const [analyzing, setAnalyzing] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [selected, setSelected] = useState(null)

  function handleValidated(f, v) {
    setFile(f)
    setValidation(v)
    setResult(null)
    setError(null)
  }

  function handleClear() {
    setFile(null)
    setValidation(null)
    setResult(null)
    setError(null)
  }

  async function analyze() {
    if (!file) return
    setAnalyzing(true)
    setError(null)
    try {
      const res = await api.analyzeCsv(file)
      setResult(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setAnalyzing(false)
    }
  }

  const canAnalyze = file && validation?.valid && !validation?.error

  return (
    <>
      <Topbar title="Traffic Analyzer" subtitle="Upload a network-flow CSV and run it through the ML pipeline" />
      <div className="page-content">
        <div className="card card-padded" style={{ marginBottom: 20 }}>
          <FileUploader onValidated={handleValidated} onClear={handleClear} />
          <button
            className="btn btn-primary btn-block"
            style={{ marginTop: 16 }}
            disabled={!canAnalyze || analyzing}
            onClick={analyze}
          >
            <PlayCircle size={16} /> {analyzing ? 'Analyzing…' : 'Analyze Traffic'}
          </button>
        </div>

        {analyzing && <LoadingState message="Running flows through preprocessing → ML model → risk scoring…" />}
        {!analyzing && error && <ErrorState message={error} onRetry={analyze} />}

        {!analyzing && !error && result && (
          <>
            <div className="grid-4" style={{ marginBottom: 20 }}>
              <div className="stat-card">
                <div className="stat-card-label">Total Flows Analyzed</div>
                <div className="stat-card-value">{result.totalFlowsAnalyzed}</div>
              </div>
              {CLASS_ORDER.map((cls) => (
                <div className="stat-card" key={cls}>
                  <div className="stat-card-label">{CLASS_LABEL[cls]}</div>
                  <div className={`stat-card-value${cls !== 'Normal' && result.classCounts[cls] > 0 ? ' accent-critical' : ''}`}>
                    {result.classCounts[cls] ?? 0}
                  </div>
                </div>
              ))}
            </div>

            <div className="card card-padded">
              <div className="card-title">Detected Threats — click a row to inspect</div>
              {result.detectedThreats.length === 0 ? (
                <EmptyState title="No threats detected" message="Every flow in this file was classified as Normal." />
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>Threat</th><th>Source IP</th><th>Destination IP</th>
                      <th>Confidence</th><th>Risk</th><th>Severity</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.detectedThreats.map((t) => (
                      <tr key={t.id} onClick={() => setSelected(t)}>
                        <td>{CLASS_LABEL[t.threatType] || t.threatType}</td>
                        <td className="mono">{t.sourceIP}</td>
                        <td className="mono">{t.destinationIP}</td>
                        <td><ConfidenceBadge confidence={t.confidence} /></td>
                        <td className="mono">{t.riskScore}</td>
                        <td><ThreatBadge severity={t.severity} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              {result.validationWarnings?.length > 0 && (
                <div style={{ marginTop: 14, fontSize: 12, color: 'var(--medium-fg)' }}>
                  {result.validationWarnings.length} row(s) produced warnings during parsing.
                </div>
              )}
            </div>
          </>
        )}
      </div>

      {selected && <ThreatDetails threat={selected} onClose={() => setSelected(null)} />}
    </>
  )
}
