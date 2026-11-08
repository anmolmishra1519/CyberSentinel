export default function ConfidenceBadge({ confidence }) {
  const pct = Math.round((confidence || 0) * 100)
  return <span className="badge badge-outline mono">{pct}% confidence</span>
}
