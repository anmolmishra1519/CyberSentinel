export default function StatCard({ label, value, accent, icon: Icon, sublabel }) {
  return (
    <div className="stat-card">
      <div className="stat-card-label">
        {label}
        {Icon && <Icon size={15} color="var(--ink-400)" />}
      </div>
      <div className={`stat-card-value${accent ? ` accent-${accent}` : ''}`}>{value}</div>
      {sublabel && <div style={{ fontSize: 11.5, color: 'var(--ink-500)' }}>{sublabel}</div>}
    </div>
  )
}
