import { useState, useEffect, useRef } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import * as complianceService from '../services/complianceService'
import { StatusBadge, PriorityBadge } from '../components/StatusBadge'
import { formatDate, isOverdue } from '../utils/helpers'

// ── Typing effect hook ────────────────────────────────────────────────────────
function useTypingEffect(text, speed = 12) {
  const [displayed, setDisplayed] = useState('')
  const [done, setDone] = useState(false)

  useEffect(() => {
    if (!text) { setDisplayed(''); setDone(false); return }
    setDisplayed('')
    setDone(false)
    let i = 0
    const interval = setInterval(() => {
      i++
      setDisplayed(text.slice(0, i))
      if (i >= text.length) { clearInterval(interval); setDone(true) }
    }, speed)
    return () => clearInterval(interval)
  }, [text, speed])

  return { displayed, done }
}

// ── Parse AI text into structured sections ────────────────────────────────────
function parseAiSections(text) {
  if (!text) return []
  const lines = text.split('\n').filter(l => l.trim())
  const sections = []
  let current = null

  for (const line of lines) {
    const trimmed = line.trim()
    // Detect numbered headings like "1) Risk assessment" or "1. Risk"
    const headingMatch = trimmed.match(/^(\d+)[.)]\s+(.+)/)
    if (headingMatch) {
      if (current) sections.push(current)
      current = { heading: headingMatch[2], bullets: [] }
    } else if (trimmed.startsWith('•') || trimmed.startsWith('-') || trimmed.startsWith('*')) {
      if (!current) current = { heading: 'Analysis', bullets: [] }
      current.bullets.push(trimmed.replace(/^[•\-*]\s*/, ''))
    } else if (trimmed.length > 0) {
      if (!current) current = { heading: 'Analysis', bullets: [] }
      current.bullets.push(trimmed)
    }
  }
  if (current) sections.push(current)
  return sections.length > 0 ? sections : [{ heading: 'AI Analysis', bullets: [text] }]
}

// ── Section icons ─────────────────────────────────────────────────────────────
const SECTION_ICONS = {
  0: { bg: 'bg-red-50',     icon: 'text-red-500',     path: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z' },
  1: { bg: 'bg-blue-50',    icon: 'text-blue-500',    path: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4' },
  2: { bg: 'bg-emerald-50', icon: 'text-emerald-500', path: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z' },
  3: { bg: 'bg-amber-50',   icon: 'text-amber-500',   path: 'M15 12a3 3 0 11-6 0 3 3 0 016 0z M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z' },
}

// ── AI Structured Output ──────────────────────────────────────────────────────
function AiStructuredOutput({ text }) {
  const { displayed, done } = useTypingEffect(text, 8)
  const sections = parseAiSections(displayed)

  return (
    <div className="space-y-3">
      {sections.map((section, idx) => {
        const style = SECTION_ICONS[idx] ?? SECTION_ICONS[3]
        return (
          <div key={idx} className="bg-white rounded-xl border border-slate-200 p-4">
            <div className="flex items-center gap-2.5 mb-3">
              <div className={`w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0 ${style.bg}`}>
                <svg className={`w-4 h-4 ${style.icon}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d={style.path} />
                </svg>
              </div>
              <h4 className="text-sm font-semibold text-slate-800">{section.heading}</h4>
            </div>
            <ul className="space-y-1.5">
              {section.bullets.map((bullet, bi) => (
                <li key={bi} className="flex items-start gap-2 text-sm text-slate-600">
                  <span className="w-1.5 h-1.5 rounded-full bg-violet-400 flex-shrink-0 mt-1.5" />
                  {bullet}
                </li>
              ))}
            </ul>
          </div>
        )
      })}
      {!done && (
        <div className="flex items-center gap-1.5 px-2 py-1">
          {[0, 1, 2].map(i => (
            <span key={i} className="w-1.5 h-1.5 rounded-full bg-violet-400 animate-bounce"
              style={{ animationDelay: `${i * 150}ms` }} />
          ))}
        </div>
      )}
    </div>
  )
}

// ── Detail row ────────────────────────────────────────────────────────────────
function DetailRow({ label, children }) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-start gap-1 sm:gap-4 py-3 border-b border-slate-100 last:border-0">
      <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider sm:w-32 flex-shrink-0 pt-0.5">
        {label}
      </span>
      <div className="flex-1 text-sm text-slate-800">{children}</div>
    </div>
  )
}

// ── AI Status indicator ───────────────────────────────────────────────────────
function AiStatusBadge({ status }) {
  if (!status) return null
  const map = {
    success:  { cls: 'bg-emerald-50 text-emerald-700 ring-emerald-200', label: 'Live AI' },
    fallback: { cls: 'bg-amber-50 text-amber-700 ring-amber-200',       label: 'Fallback Mode' },
    offline:  { cls: 'bg-slate-100 text-slate-600 ring-slate-200',      label: 'Offline Mode' },
  }
  const s = map[status] ?? map.offline
  return (
    <span className={`badge text-[10px] ${s.cls}`}>
      <span className="w-1.5 h-1.5 rounded-full bg-current" />
      {s.label}
    </span>
  )
}

// ── Main ──────────────────────────────────────────────────────────────────────
export default function ComplianceDetail() {
  const { id }   = useParams()
  const navigate = useNavigate()

  const [record,    setRecord]    = useState(null)
  const [loading,   setLoading]   = useState(true)
  const [error,     setError]     = useState(null)
  const [deleting,  setDeleting]  = useState(false)

  const [aiText,    setAiText]    = useState('')
  const [aiStatus,  setAiStatus]  = useState(null)   // 'success' | 'fallback' | 'offline'
  const [aiLoading, setAiLoading] = useState(false)
  const [aiError,   setAiError]   = useState('')
  const [aiAsked,   setAiAsked]   = useState(false)
  const [retryCount, setRetryCount] = useState(0)

  const aiRef = useRef(null)

  useEffect(() => {
    complianceService.getById(id)
      .then(r => setRecord(r.data))
      .catch(err => setError(err.response?.data?.message || 'Failed to load record.'))
      .finally(() => setLoading(false))
  }, [id])

  const handleDelete = async () => {
    if (!window.confirm(`Delete "${record?.title}"?\nThis cannot be undone.`)) return
    setDeleting(true)
    try {
      await complianceService.remove(id)
      navigate('/compliance')
    } catch (err) {
      alert(err.response?.data?.message || 'Delete failed.')
      setDeleting(false)
    }
  }

  const handleAskAi = async () => {
    setAiAsked(true)
    setAiLoading(true)
    setAiError('')
    setAiText('')
    setAiStatus(null)
    // Scroll to AI panel
    setTimeout(() => aiRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 100)
    try {
      const res = await complianceService.getAiAnalysis(id)
      const data = res.data
      setAiText(data?.analysis ?? data?.result ?? data?.description ?? JSON.stringify(data, null, 2))
      setAiStatus(data?.status ?? 'success')
      setRetryCount(0)
    } catch (err) {
      const msg = err.response?.data?.message || 'AI analysis failed. Please try again.'
      setAiError(msg)
      setRetryCount(c => c + 1)
    } finally {
      setAiLoading(false)
    }
  }

  // ── Loading ──
  if (loading) {
    return (
      <div className="max-w-3xl mx-auto space-y-4 animate-pulse">
        <div className="h-8 bg-slate-200 rounded-xl w-56" />
        <div className="card space-y-3">
          {[1,2,3,4,5].map(i => <div key={i} className="h-8 bg-slate-100 rounded-xl" />)}
        </div>
      </div>
    )
  }

  // ── Error ──
  if (error) {
    return (
      <div className="max-w-3xl mx-auto">
        <div className="card text-center py-16">
          <div className="w-14 h-14 bg-red-50 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <svg className="w-7 h-7 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <p className="text-slate-700 font-semibold mb-1">Failed to load record</p>
          <p className="text-sm text-slate-400 mb-6">{error}</p>
          <button onClick={() => navigate('/compliance')} className="btn-secondary">← Back to Records</button>
        </div>
      </div>
    )
  }

  const overdue = isOverdue(record.dueDate) && record.status !== 'COMPLIANT'

  return (
    <div className="page-shell max-w-3xl mx-auto space-y-6">

      {/* ── Header ── */}
      <div className="flex items-start gap-3">
        <button onClick={() => navigate('/compliance')} className="btn-secondary p-2 mt-0.5">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-2 mb-1">
            <h1 className="page-title truncate">{record.title}</h1>
            {overdue && <span className="badge bg-red-100 text-red-700 ring-red-200">Overdue</span>}
          </div>
          <p className="page-subtitle">Record #{record.id}</p>
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
          <button onClick={() => navigate(`/compliance/${id}/edit`)} className="btn-secondary">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            Edit
          </button>
          <button onClick={handleDelete} disabled={deleting} className="btn-danger">
            {deleting ? (
              <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
              </svg>
            ) : (
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            )}
            Delete
          </button>
        </div>
      </div>

      {/* ── Record Details ── */}
      <div className="card">
        <h2 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-3">Record Details</h2>
        <DetailRow label="Title">{record.title}</DetailRow>
        <DetailRow label="Status"><StatusBadge status={record.status} /></DetailRow>
        <DetailRow label="Priority"><PriorityBadge priority={record.priority} /></DetailRow>
        <DetailRow label="Due Date">
          <span className={overdue ? 'text-red-600 font-semibold' : ''}>
            {formatDate(record.dueDate)}
            {overdue && <span className="ml-2 text-xs text-red-400 font-semibold uppercase tracking-wide">Overdue</span>}
          </span>
        </DetailRow>
        {record.score != null && (
          <DetailRow label="Score">
            <div className="flex items-center gap-3">
              <div className="flex-1 max-w-[160px] h-2 bg-slate-100 rounded-full overflow-hidden">
                <div
                  className={`h-full rounded-full ${record.score >= 75 ? 'bg-emerald-500' : record.score >= 40 ? 'bg-amber-400' : 'bg-red-500'}`}
                  style={{ width: `${Math.min(100, record.score)}%` }}
                />
              </div>
              <span className="text-sm font-mono font-semibold text-slate-600">{record.score}</span>
            </div>
          </DetailRow>
        )}
        {record.description && (
          <DetailRow label="Description">
            <p className="text-slate-600 leading-relaxed">{record.description}</p>
          </DetailRow>
        )}
        {record.createdAt && <DetailRow label="Created">{formatDate(record.createdAt)}</DetailRow>}
        {record.updatedAt && <DetailRow label="Updated">{formatDate(record.updatedAt)}</DetailRow>}
      </div>

      {/* ── AI Analysis Panel ── */}
      <div ref={aiRef} className="card border-violet-100">

        {/* Panel header */}
        <div className="flex items-center justify-between mb-5">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center shadow-sm">
              <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8}
                  d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
              </svg>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-slate-800">AI Analysis</h2>
                <AiStatusBadge status={aiStatus} />
              </div>
              <p className="text-xs text-slate-400">Powered by Groq · llama-3.1-8b-instant</p>
            </div>
          </div>

          {!aiLoading && (
            <button
              onClick={handleAskAi}
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-violet-600 to-purple-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:from-violet-700 hover:to-purple-700 transition-all focus:outline-none focus:ring-2 focus:ring-violet-400"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              {aiAsked ? 'Re-analyse' : 'Ask AI'}
              {retryCount > 0 && <span className="text-violet-200 text-xs">({retryCount})</span>}
            </button>
          )}
        </div>

        {/* ── Idle state ── */}
        {!aiAsked && (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-violet-50 to-purple-50 flex items-center justify-center mb-4 border border-violet-100">
              <svg className="w-8 h-8 text-violet-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                  d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
            </div>
            <p className="text-sm font-medium text-slate-700 mb-1">Get AI-powered insights</p>
            <p className="text-xs text-slate-400 max-w-xs">
              Click <strong className="text-violet-600">Ask AI</strong> to receive a structured analysis including
              risk assessment, recommended actions, compliance score, and key observations.
            </p>
          </div>
        )}

        {/* ── Loading state ── */}
        {aiLoading && (
          <div className="space-y-3">
            <div className="flex items-center gap-3 px-4 py-3 bg-violet-50 rounded-xl border border-violet-100">
              <div className="flex items-center gap-1.5">
                {[0,1,2].map(i => (
                  <span key={i} className="w-2 h-2 rounded-full bg-violet-500 animate-bounce"
                    style={{ animationDelay: `${i * 150}ms` }} />
                ))}
              </div>
              <p className="text-sm text-violet-700 font-medium">Analysing compliance record with AI…</p>
            </div>
            {/* Skeleton cards */}
            {[1,2,3,4].map(i => (
              <div key={i} className="bg-white rounded-xl border border-slate-200 p-4 animate-pulse"
                style={{ animationDelay: `${i * 100}ms` }}>
                <div className="flex items-center gap-2.5 mb-3">
                  <div className="w-7 h-7 rounded-lg bg-slate-100" />
                  <div className="h-3 bg-slate-100 rounded-full w-32" />
                </div>
                <div className="space-y-2">
                  <div className="h-2.5 bg-slate-100 rounded-full w-full" />
                  <div className="h-2.5 bg-slate-100 rounded-full w-4/5" />
                  <div className="h-2.5 bg-slate-100 rounded-full w-3/5" />
                </div>
              </div>
            ))}
          </div>
        )}

        {/* ── Error state ── */}
        {aiError && !aiLoading && (
          <div className="space-y-3">
            <div className="flex items-start gap-3 bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3.5 rounded-xl">
              <svg className="w-5 h-5 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div>
                <p className="font-semibold mb-0.5">Analysis failed</p>
                <p className="text-red-600 text-xs">{aiError}</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <button onClick={handleAskAi}
                className="btn-secondary text-sm">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                Retry {retryCount > 0 && `(${retryCount})`}
              </button>
              <p className="text-xs text-slate-400">Make sure the AI service is running on port 5000</p>
            </div>
          </div>
        )}

        {/* ── AI Response ── */}
        {aiText && !aiLoading && (
          <div>
            <div className="flex items-center gap-2 mb-4">
              <div className="w-5 h-5 rounded-full bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center">
                <svg className="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M11.3 1.046A1 1 0 0112 2v5h4a1 1 0 01.82 1.573l-7 10A1 1 0 018 18v-5H4a1 1 0 01-.82-1.573l7-10a1 1 0 011.12-.38z" clipRule="evenodd" />
                </svg>
              </div>
              <span className="text-xs font-bold text-violet-700 uppercase tracking-wider">AI Response</span>
              <span className="text-xs text-slate-400">· {new Date().toLocaleTimeString()}</span>
            </div>
            <AiStructuredOutput text={aiText} />
          </div>
        )}
      </div>
    </div>
  )
}
