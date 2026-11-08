import { useState } from 'react'
import Topbar from '../components/Topbar'
import SimulatorControls from '../components/SimulatorControls'
import ThreatDetails from '../components/ThreatDetails'
import ThreatBadge from '../components/ThreatBadge'
import ConfidenceBadge from '../components/ConfidenceBadge'
import RiskScore from '../components/RiskScore'
import LoadingState from '../components/LoadingState'
import ErrorState from '../components/ErrorState'
import api from '../services/api'
import { Sparkles } from 'lucide-react'

const LABEL = { PortScan: 'Port Scan', BruteForce: 'Brute Force', DDoS: 'DDoS / DoS', Normal: 'Normal' }

export default function AttackSimulator() {
  const [status, setStatus] = useState('NORMAL')
  const [running, setRunning] = useState(false)
  const [lastResult, setLastResult] = useState(null)
  const [error, setError] = useState(null)
  const [showDetail, setShowDetail] = useState(false)

  async function run(attackType) {
    setRunning(true)
    setError(null)
    setLastResult(null)
    try {
      const res = await api.startSimulation(attackType)
      setLastResult(res)
      setStatus(res.headlineThreat && res.headlineThreat.threatType !== 'Normal' ? res.headlineThreat.threatType : 'NORMAL')
    } catch (err) {
      setError(err.message)
    } finally {
      setRunning(false)
    }
  }

  const headline = lastResult?.headlineThreat

  return (
    <>
      <Topbar title="Attack Simulator" subtitle="Generates real flow records through the same ML pipeline as an upload" />
      <div className="page-content">
        <div className="card card-padded" style={{ marginBottom: 20 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 18 }}>
            <div className="card-title" style={{ marginBottom: 0 }}>Current Status</div>
            <span className={`status-pill${status !== 'NORMAL' ? ' alert' : ''}`}>
              <span className="pulse-dot" />
              {status === 'NORMAL' ? 'NORMAL' : `${LABEL[status] || status} DETECTED`}
            </span>
          </div>
          <SimulatorControls onRun={run} busy={running} />
        </div>

        {running && <LoadingState message="Generating flow records → preprocessing → ML model → risk scoring…" />}
        {!running && error && <ErrorState message={error} onRetry={() => run('NORMAL')} />}

        {!running && !error && headline && headline.threatType !== 'Normal' && (
          <div className="card card-padded" style={{ borderColor: 'var(--critical-border)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16 }}>
              <RiskScore value={headline.riskScore} size={64} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 17, fontWeight: 700 }}>{LABEL[headline.threatType]} Detected</div>
                <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
                  <ConfidenceBadge confidence={headline.confidence} />
                  <ThreatBadge severity={headline.severity} />
                  <span className="badge badge-outline">{lastResult.flowsGenerated} flows generated</span>
                </div>
              </div>
              <button className="btn btn-primary" onClick={() => setShowDetail(true)}>
                <Sparkles size={15} /> View Details
              </button>
            </div>
          </div>
        )}

        {!running && !error && lastResult && (!headline || headline.threatType === 'Normal') && (
          <div className="card card-padded">
            <div style={{ fontSize: 13.5, color: 'var(--ink-700)' }}>
              {lastResult.flowsGenerated} flow(s) generated and classified as Normal traffic. No threat raised.
            </div>
          </div>
        )}
      </div>

      {showDetail && headline && (
        <ThreatDetails threat={headline} onClose={() => setShowDetail(false)} />
      )}
    </>
  )
}
