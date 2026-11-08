import { NavLink } from 'react-router-dom'
import { LayoutDashboard, Activity, ShieldAlert, Radio, ServerCog } from 'lucide-react'

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/traffic-analyzer', label: 'Traffic Analyzer', icon: Activity },
  { to: '/threats', label: 'Threats', icon: ShieldAlert },
  { to: '/attack-simulator', label: 'Attack Simulator', icon: Radio },
  { to: '/system-status', label: 'System Status', icon: ServerCog },
]

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="sidebar-brand-mark">CS</div>
        <div className="sidebar-brand-text">
          <strong>CyberSentinel</strong>
          <span>AI THREAT DETECTION</span>
        </div>
      </div>

      {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
        >
          <Icon size={17} strokeWidth={2} />
          {label}
        </NavLink>
      ))}

      <div className="sidebar-footer">
        <div style={{ fontSize: 11, color: 'var(--ink-400)', lineHeight: 1.5 }}>
          SIH26145 · Blockchain &amp; Cybersecurity<br />
          College Prototype
        </div>
      </div>
    </aside>
  )
}
