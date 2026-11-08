import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import EmptyState from './EmptyState'

export default function TrafficChart({ data }) {
  if (!data || data.length === 0) {
    return <EmptyState title="No traffic yet" message="Upload a CSV or run the Attack Simulator to see live activity." />
  }

  return (
    <ResponsiveContainer width="100%" height={240}>
      <AreaChart data={data} margin={{ top: 4, right: 8, left: -18, bottom: 0 }}>
        <defs>
          <linearGradient id="flowsGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--brand-500)" stopOpacity={0.25} />
            <stop offset="100%" stopColor="var(--brand-500)" stopOpacity={0} />
          </linearGradient>
          <linearGradient id="threatsGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--critical-fg)" stopOpacity={0.25} />
            <stop offset="100%" stopColor="var(--critical-fg)" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid vertical={false} stroke="var(--border)" />
        <XAxis dataKey="label" tick={{ fontSize: 11, fill: 'var(--ink-500)' }} axisLine={false} tickLine={false} />
        <YAxis tick={{ fontSize: 11, fill: 'var(--ink-500)' }} axisLine={false} tickLine={false} width={30} />
        <Tooltip
          contentStyle={{ borderRadius: 10, border: '1px solid var(--border)', fontSize: 12.5 }}
        />
        <Area type="monotone" dataKey="flows" name="Flows" stroke="var(--brand-500)" fill="url(#flowsGrad)" strokeWidth={2} />
        <Area type="monotone" dataKey="threats" name="Threats" stroke="var(--critical-fg)" fill="url(#threatsGrad)" strokeWidth={2} />
      </AreaChart>
    </ResponsiveContainer>
  )
}
