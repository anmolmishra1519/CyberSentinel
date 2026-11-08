import { CheckCircle2 } from 'lucide-react'

export default function ExplanationPanel({ reasons, recommendedAction }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <div>
        <div className="card-title" style={{ marginBottom: 12 }}>WHY WAS THIS DETECTED?</div>
        <div className="reason-list">
          {(reasons || []).map((r, i) => (
            <div key={i} className="reason-item">
              <span className="reason-check"><CheckCircle2 size={13} /></span>
              {r}
            </div>
          ))}
        </div>
      </div>

      {recommendedAction && (
        <div>
          <div className="card-title" style={{ marginBottom: 10 }}>RECOMMENDED ACTION</div>
          <div className="action-box">{recommendedAction}</div>
        </div>
      )}
    </div>
  )
}
