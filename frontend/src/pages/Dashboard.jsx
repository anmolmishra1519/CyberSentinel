import { useEffect, useState, useCallback } from 'react'
import { Activity, ShieldAlert, AlertOctagon, Gauge } from 'lucide-react'
import api from '../services/api'
import Topbar from '../components/Topbar'
import StatCard from '../components/StatCard'
import TrafficChart from '../components/TrafficChart'
import ThreatChart from '../components/ThreatChart'
import ThreatFeed from '../components/ThreatFeed'
import ThreatDetails from '../components/ThreatDetails'
import LoadingState from '../components/LoadingState'
import ErrorState from '../components/ErrorState'

export default function Dashboard() {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [selected, setSelected] = useState(null)

  const load = useCallback(async () => {
    try {
      setError(null)
      const data = await api.getDashboardStats()
      setStats(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    const id = setInterval(load, 6000)
    return () => clearInterval(id)
  }, [load])

  async function openThreat(t) {
    try {
      const full = await api.getThreat(t.id)
      setSelected(full)
    } catch {
      setSelected(t)
    }
  }

  return (
    <>
      <Topbar title="CYBERSENTINEL AI" subtitle="AI-Powered Network Threat Detection" />
      <div className="page-content">
        {loading && <LoadingState message="Loading dashboard…" />}
        {!loading && error && <ErrorState message={error} onRetry={load} />}

        {!loading && !error && stats && (
          <>
            <div className="grid-4" style={{ marginBottom: 16 }}>
              <StatCard label="Total Flows" value={stats.totalFlows} icon={Activity} />
              <StatCard label="Threats Detected" value={stats.threatsDetected} icon={ShieldAlert} />
              <StatCard
                label="Critical Threats"
                value={stats.criticalThreats}
                accent={stats.criticalThreats > 0 ? 'critical' : undefined}
                icon={AlertOctagon}
              />
              <StatCard label="Current Risk" value={stats.currentRisk} icon={Gauge} />
            </div>

            <div className="grid-2" style={{ marginBottom: 16 }}>
              <div className="card card-padded">
                <div className="card-title">Traffic Activity</div>
                <TrafficChart data={stats.trafficActivity} />
              </div>
              <div className="card card-padded">
                <div className="card-title">Threat Distribution</div>
                <ThreatChart distribution={stats.threatDistribution} />
              </div>
            </div>

            <div className="card card-padded">
              <div className="card-title">Live Threat Feed</div>
              <ThreatFeed threats={stats.liveThreatFeed} onSelect={openThreat} />
            </div>
          </>
        )}
      </div>

      {selected && <ThreatDetails threat={selected} onClose={() => setSelected(null)} />}
    </>
  )
}
