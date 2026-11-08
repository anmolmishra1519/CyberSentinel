import { useEffect, useState } from 'react'
import api from '../services/api'

export default function Topbar({ title, subtitle }) {
  const [status, setStatus] = useState(null)

  useEffect(() => {
    let mounted = true
    async function poll() {
      try {
        const s = await api.getSimulationStatus()
        if (mounted) setStatus(s.status)
      } catch {
        if (mounted) setStatus(null)
      }
    }
    poll()
    const id = setInterval(poll, 5000)
    return () => { mounted = false; clearInterval(id) }
  }, [])

  const isAlert = status && status !== 'NORMAL'

  return (
    <header className="topbar">
      <div>
        <div className="topbar-title">{title}</div>
        {subtitle && <div className="topbar-subtitle">{subtitle}</div>}
      </div>
      <div className={`status-pill${isAlert ? ' alert' : ''}`}>
        <span className="pulse-dot" />
        {isAlert ? `${formatLabel(status)} DETECTED` : 'SYSTEM MONITORING'}
      </div>
    </header>
  )
}

function formatLabel(type) {
  if (!type) return ''
  if (type === 'PortScan') return 'PORT SCAN'
  if (type === 'BruteForce') return 'BRUTE FORCE'
  return type.toUpperCase()
}
