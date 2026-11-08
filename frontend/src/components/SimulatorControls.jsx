import { Wifi, Zap, Search, KeyRound } from 'lucide-react'

const OPTIONS = [
  { type: 'NORMAL', title: 'Start Normal Traffic', desc: 'Everyday client/server sessions', icon: Wifi },
  { type: 'DDOS', title: 'Simulate DDoS / DoS', desc: 'Burst of high-rate flows at one destination', icon: Zap },
  { type: 'PORT_SCAN', title: 'Simulate Port Scan', desc: 'One source sweeping many destination ports', icon: Search },
  { type: 'BRUTE_FORCE', title: 'Simulate Brute Force', desc: 'Repeated attempts at an auth service port', icon: KeyRound },
]

export default function SimulatorControls({ onRun, busy }) {
  return (
    <div className="sim-buttons">
      {OPTIONS.map(({ type, title, desc, icon: Icon }) => (
        <button key={type} className="sim-btn" disabled={busy} onClick={() => onRun(type)}>
          <Icon size={18} color="var(--brand-700)" style={{ marginBottom: 8 }} />
          <div className="sim-btn-title">{title}</div>
          <div className="sim-btn-desc">{desc}</div>
        </button>
      ))}
    </div>
  )
}
