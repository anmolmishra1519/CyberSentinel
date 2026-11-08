import { useRef, useState } from 'react'
import { UploadCloud, FileCheck2, XCircle } from 'lucide-react'
import api from '../services/api'

export default function FileUploader({ onValidated, onClear }) {
  const inputRef = useRef(null)
  const [file, setFile] = useState(null)
  const [validating, setValidating] = useState(false)
  const [validation, setValidation] = useState(null) // { valid, flowCount, warnings } | { error }

  async function handleFile(selected) {
    if (!selected) return
    if (!selected.name.toLowerCase().endsWith('.csv')) {
      setValidation({ error: 'Only .csv files are accepted.' })
      return
    }
    setFile(selected)
    setValidating(true)
    setValidation(null)
    try {
      const result = await api.validateCsv(selected)
      setValidation(result)
      onValidated?.(selected, result)
    } catch (err) {
      setValidation({ error: err.message })
    } finally {
      setValidating(false)
    }
  }

  function reset() {
    setFile(null)
    setValidation(null)
    if (inputRef.current) inputRef.current.value = ''
    onClear?.()
  }

  return (
    <div>
      <div
        className="upload-zone"
        onDragOver={(e) => e.preventDefault()}
        onDrop={(e) => { e.preventDefault(); handleFile(e.dataTransfer.files?.[0]) }}
      >
        <UploadCloud size={28} color="var(--brand-500)" style={{ marginBottom: 10 }} />
        <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 4 }}>Upload Network Flow Dataset</div>
        <div style={{ fontSize: 12.5, color: 'var(--ink-500)', marginBottom: 16 }}>
          CSV with sourceIP, destinationIP, protocol, sourcePort, destinationPort,
          flowDuration, packetCount, byteCount
        </div>
        <input
          ref={inputRef}
          type="file"
          accept=".csv"
          style={{ display: 'none' }}
          onChange={(e) => handleFile(e.target.files?.[0])}
        />
        <button className="btn btn-primary" onClick={() => inputRef.current?.click()}>Choose CSV</button>
      </div>

      {file && (
        <div className="card card-padded" style={{ marginTop: 14 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              {validating ? (
                <div className="spinner" />
              ) : validation?.error ? (
                <XCircle size={18} color="var(--critical-fg)" />
              ) : (
                <FileCheck2 size={18} color="var(--safe-fg)" />
              )}
              <div>
                <div style={{ fontWeight: 600, fontSize: 13.5 }}>{file.name}</div>
                <div style={{ fontSize: 12, color: 'var(--ink-500)' }}>
                  {validating
                    ? 'Validating…'
                    : validation?.error
                      ? validation.error
                      : `${validation?.flowCount ?? 0} flows · Validation passed`}
                </div>
              </div>
            </div>
            <button className="btn" onClick={reset}>Remove</button>
          </div>
          {validation?.warnings?.length > 0 && (
            <div style={{ marginTop: 10, fontSize: 12, color: 'var(--medium-fg)' }}>
              {validation.warnings.length} row(s) had warnings and were skipped or adjusted.
            </div>
          )}
        </div>
      )}
    </div>
  )
}
