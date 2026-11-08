import { AlertTriangle, RotateCw } from 'lucide-react'

export default function ErrorState({ message, onRetry }) {
  return (
    <div className="state-box">
      <AlertTriangle size={26} color="var(--critical-fg)" />
      <h3>Something went wrong</h3>
      <p>{message || 'An unexpected error occurred.'}</p>
      {onRetry && (
        <button className="btn" onClick={onRetry} style={{ marginTop: 6 }}>
          <RotateCw size={14} /> Retry
        </button>
      )}
    </div>
  )
}
