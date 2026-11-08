import { useEffect, useState, useCallback } from 'react'
import Topbar from '../components/Topbar'
import SystemStatus from '../components/SystemStatus'
import LoadingState from '../components/LoadingState'
import ErrorState from '../components/ErrorState'
import api from '../services/api'

export default function SystemStatusPage() {
  const [status, setStatus] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(async () => {
    try {
      setError(null)
      const data = await api.getSystemStatus()
      setStatus(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    const id = setInterval(load, 8000)
    return () => clearInterval(id)
  }, [load])

  return (
    <>
      <Topbar title="System Status" subtitle="Health of every service in the pipeline" />
      <div className="page-content">
        {loading && <LoadingState message="Checking service health…" />}
        {!loading && error && <ErrorState message={error} onRetry={load} />}
        {!loading && !error && (
          <div className="card card-padded" style={{ maxWidth: 640 }}>
            <SystemStatus status={status} />
          </div>
        )}
      </div>
    </>
  )
}
