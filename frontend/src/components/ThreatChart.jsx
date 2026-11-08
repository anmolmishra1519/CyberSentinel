import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import EmptyState from './EmptyState'

const COLORS = {
  Normal: '#98A2B3',
  DDoS: '#B42318',
  PortScan: '#B54708',
  BruteForce: '#C4320A',
}

export default function ThreatChart({ distribution }) {
  const entries = Object.entries(distribution || {}).filter(([, v]) => v > 0)
  if (entries.length === 0) {
    return <EmptyState title="No classifications yet" message="Threat distribution will appear once flows are analyzed." />
  }
  const data = entries.map(([name, value]) => ({ name, value }))

  return (
    <ResponsiveContainer width="100%" height={240}>
      <PieChart>
        <Pie data={data} dataKey="value" nameKey="name" innerRadius={58} outerRadius={86} paddingAngle={2}>
          {data.map((d) => (
            <Cell key={d.name} fill={COLORS[d.name] || '#98A2B3'} stroke="var(--surface)" strokeWidth={2} />
          ))}
        </Pie>
        <Tooltip contentStyle={{ borderRadius: 10, border: '1px solid var(--border)', fontSize: 12.5 }} />
        <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
      </PieChart>
    </ResponsiveContainer>
  )
}
