export default function ThreatBadge({ severity }) {
  const level = (severity || 'SAFE').toUpperCase()
  return <span className={`badge badge-${level}`}>{level}</span>
}
