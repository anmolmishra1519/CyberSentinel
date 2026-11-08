import { Inbox } from 'lucide-react'

export default function EmptyState({ title = 'Nothing here yet', message }) {
  return (
    <div className="state-box">
      <Inbox size={24} color="var(--ink-400)" />
      <h3>{title}</h3>
      {message && <p>{message}</p>}
    </div>
  )
}
