import { CheckCircle2, XCircle } from 'lucide-react'

function Row({ label, online, detail }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '13px 4px', borderBottom: '1px solid var(--border)',
    }}>
      <div>
        <div style={{ fontSize: 13.5, fontWeight: 600 }}>{label}</div>
        {detail && <div style={{ fontSize: 11.5, color: 'var(--ink-500)' }}>{detail}</div>}
      </div>
      <span className={`badge ${online ? 'badge-SAFE' : 'badge-CRITICAL'}`}>
        {online ? <CheckCircle2 size={12} /> : <XCircle size={12} />}
        {online ? 'Online' : 'Offline'}
      </span>
    </div>
  )
}

export default function SystemStatus({ status }) {
  if (!status) return null
  return (
    <div>
      <Row label="ML Service (FastAPI)" online={status.mlServiceOnline} detail="Random Forest inference engine" />
      <Row label="Spring Boot API" online={status.springBootOnline} detail="REST backend" />
      <Row label="MongoDB" online={status.mongoConnected} detail="network_flows / threats / alerts" />
      <Row label="Frontend" online={true} detail="React + Vite" />
      <Row label="System Monitoring" online={true} detail={`Uptime ${Math.floor((status.uptimeSeconds || 0) / 60)}m`} />
    </div>
  )
}
