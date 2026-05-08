import { useState, useRef, useEffect, useCallback } from 'react'
import * as aiService from '../services/aiService'

// ── Spinner ───────────────────────────────────────────────────────────────────
function Spinner({ size = 'md' }) {
  const s = size === 'sm' ? 'w-4 h-4' : 'w-6 h-6'
  return (
    <svg className={`${s} animate-spin text-violet-500`} fill="none" viewBox="0 0 24 24">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
    </svg>
  )
}

// ── Typing effect ─────────────────────────────────────────────────────────────
function TypingText({ text, speed = 12, onComplete }) {
  const [displayed, setDisplayed] = useState('')
  const idx = useRef(0)

  useEffect(() => {
    setDisplayed('')
    idx.current = 0
    if (!text) return
    const t = setInterval(() => {
      idx.current += 1
      setDisplayed(text.slice(0, idx.current))
      if (idx.current >= text.length) {
        clearInterval(t)
        if (onComplete) onComplete()
      }
    }, speed)
    return () => clearInterval(t)
  }, [text, speed, onComplete])

  return (
    <span className="whitespace-pre-wrap leading-relaxed">
      {displayed}
      {displayed.length < (text?.length || 0) && (
        <span className="inline-block w-2 h-4 bg-violet-500 ml-1 animate-pulse align-middle rounded-sm" />
      )}
    </span>
  )
}

// ── AI Response Card ──────────────────────────────────────────────────────────
function AIResponseCard({ data, onRetry, loading }) {
  if (loading) {
    return (
      <div className="mt-6 rounded-2xl border border-violet-200 bg-gradient-to-br from-violet-50 to-purple-50 p-6">
        <div className="flex items-center gap-3 mb-4">
          <div className="w-8 h-8 rounded-full bg-violet-100 flex items-center justify-center">
            <Spinner size="sm" />
          </div>
          <p className="text-sm font-semibold text-violet-700">AI is thinking…</p>
        </div>
        <div className="space-y-2">
          {[100, 85, 70, 90].map((w, i) => (
            <div key={i} className="h-3 bg-violet-200 rounded-full animate-pulse" style={{ width: `${w}%`, animationDelay: `${i * 150}ms` }} />
          ))}
        </div>
      </div>
    )
  }

  if (!data && !loading) return null

  return (
    <div className="mt-6 rounded-2xl border border-violet-200 bg-gradient-to-br from-violet-50 to-purple-50 p-6 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-violet-600 flex items-center justify-center text-white text-xs font-bold">AI</div>
          <div>
            <p className="text-sm font-bold text-violet-800">AI Response</p>
            {data.generated_at && (
              <p className="text-[11px] text-violet-400">{new Date(data.generated_at).toLocaleTimeString()}</p>
            )}
          </div>
        </div>
        <button onClick={onRetry} className="text-xs text-violet-500 hover:text-violet-700 flex items-center gap-1 transition-colors px-2 py-1 rounded-lg hover:bg-violet-100">
          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          Retry
        </button>
      </div>
      <div className="text-sm text-slate-700 leading-relaxed">
        <TypingText text={data.response || data.recommendations || ''} />
      </div>
    </div>
  )
}

// ── Error Alert ───────────────────────────────────────────────────────────────
function ErrorAlert({ message, onRetry }) {
  return (
    <div className="mt-4 rounded-xl border border-red-200 bg-red-50 p-4 flex items-start gap-3">
      <svg className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M12 3C7.03 3 3 7.03 3 12s4.03 9 9 9 9-4.03 9-9S16.97 3 12 3z" />
      </svg>
      <div className="flex-1">
        <p className="text-sm font-semibold text-red-700">Request failed</p>
        <p className="text-xs text-red-500 mt-0.5">{message}</p>
      </div>
      <button onClick={onRetry} className="text-xs font-semibold text-red-600 hover:text-red-800 px-3 py-1.5 rounded-lg border border-red-200 hover:bg-red-100 transition-all">
        ↻ Retry
      </button>
    </div>
  )
}

// ── Streaming Report ──────────────────────────────────────────────────────────
function StreamingReport() {
  const [prompt, setPrompt] = useState('')
  const [status, setStatus] = useState('idle') // idle | pending | polling | done | error
  const [reportId, setReportId] = useState(null)
  const [reportData, setReportData] = useState(null)
  const [error, setError] = useState(null)
  const pollRef = useRef(null)
  const [progressLog, setProgressLog] = useState('')

  const stopPolling = () => { if (pollRef.current) clearInterval(pollRef.current) }

  const startPolling = useCallback((id) => {
    setStatus('polling')
    let attempts = 0
    pollRef.current = setInterval(async () => {
      attempts++
      setProgressLog(prev => prev + `\n> Polling status [Attempt ${attempts}]...`)
      try {
        const res = await aiService.getReport(id)
        const r = res.data
        if (r.status === 'DONE') {
          stopPolling()
          setProgressLog(prev => prev + `\n> Report generation complete. Formulating final output...`)
          setTimeout(() => {
            setReportData(r.data)
            setStatus('done')
          }, 800)
        } else if (r.status === 'FAILED') {
          stopPolling()
          setError('Report generation failed on server.')
          setStatus('error')
        }
      } catch (e) {
        stopPolling()
        setError(e.response?.data?.error || 'Could not fetch report status.')
        setStatus('error')
      }
    }, 2500)
  }, [])

  useEffect(() => () => stopPolling(), [])

  const handleGenerate = async () => {
    if (!prompt.trim()) return
    stopPolling()
    setStatus('pending')
    setError(null)
    setReportData(null)
    setProgressLog('> Initializing connection to AI engine...\n> Submitting parameters...')
    try {
      const res = await aiService.generateReport(prompt)
      const { report_id } = res.data
      setReportId(report_id)
      setProgressLog(prev => prev + `\n> Request accepted. Report ID: ${report_id}\n> Starting generation sequence...`)
      startPolling(report_id)
    } catch (e) {
      setError(e.response?.data?.error || 'Failed to start report generation.')
      setStatus('error')
    }
  }

  const handleReset = () => {
    stopPolling()
    setStatus('idle')
    setReportData(null)
    setError(null)
    setReportId(null)
    setPrompt('')
    setProgressLog('')
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
        <label className="block text-sm font-semibold text-slate-700 mb-2">Report Topic</label>
        <textarea
          id="stream-prompt"
          rows={3}
          placeholder="e.g. Generate a comprehensive compliance summary for Q1 2025 detailing any outstanding tasks..."
          value={prompt}
          onChange={e => setPrompt(e.target.value)}
          disabled={status === 'pending' || status === 'polling'}
          className="w-full px-4 py-3 text-sm rounded-xl border border-slate-200 bg-slate-50 outline-none focus:border-violet-400 focus:ring-2 focus:ring-violet-100 focus:bg-white transition resize-none disabled:opacity-60 mb-4"
        />
        <div className="flex gap-3">
          <button
            id="generate-report-btn"
            onClick={handleGenerate}
            disabled={!prompt.trim() || status === 'pending' || status === 'polling'}
            className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-violet-600 text-white text-sm font-semibold hover:bg-violet-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-md shadow-violet-200"
          >
            {(status === 'pending' || status === 'polling') ? <Spinner size="sm" /> : (
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            )}
            {status === 'pending' ? 'Starting…' : status === 'polling' ? 'Generating…' : 'Generate Report'}
          </button>
          {status !== 'idle' && (
            <button onClick={handleReset} className="px-5 py-2.5 rounded-xl border border-slate-200 text-slate-600 font-semibold text-sm hover:bg-slate-50 hover:text-slate-900 transition-all">
              Reset
            </button>
          )}
        </div>
      </div>

      {/* Streaming UI Container */}
      {status !== 'idle' && (
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden flex flex-col">
          {/* Mac-like terminal header */}
          <div className="bg-slate-50 border-b border-slate-200 px-4 py-3 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="flex gap-1.5">
                <div className="w-3 h-3 rounded-full bg-red-400"></div>
                <div className="w-3 h-3 rounded-full bg-amber-400"></div>
                <div className="w-3 h-3 rounded-full bg-emerald-400"></div>
              </div>
              <span className="text-xs font-medium text-slate-500 font-mono tracking-tight">ai-report-generator.sh</span>
            </div>
            <div className="flex items-center gap-2">
              {(status === 'pending' || status === 'polling') && (
                <div className="flex items-center gap-1.5 px-2 py-1 rounded-md bg-amber-50 text-amber-600 text-xs font-semibold border border-amber-200">
                  <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></span>
                  Processing
                </div>
              )}
              {status === 'done' && (
                <div className="flex items-center gap-1.5 px-2 py-1 rounded-md bg-emerald-50 text-emerald-600 text-xs font-semibold border border-emerald-200">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                  Completed
                </div>
              )}
            </div>
          </div>
          
          {/* Terminal/Document Body */}
          <div className="p-6 bg-[#0f172a] text-slate-300 font-mono text-sm leading-relaxed min-h-[250px] max-h-[500px] overflow-y-auto">
            {/* Progress Log */}
            {(status === 'pending' || status === 'polling') && (
              <div className="text-emerald-400 mb-4 whitespace-pre-wrap opacity-80">
                <TypingText text={progressLog} speed={15} />
              </div>
            )}
            
            {/* Error State */}
            {status === 'error' && (
              <div className="text-red-400 mt-2">
                {'>'} Error encountered during generation sequence.<br/>
                {'>'} {error}
              </div>
            )}
            
            {/* Final Report with typing effect */}
            {status === 'done' && reportData && (
              <div className="animate-in fade-in slide-in-from-bottom-2 duration-500">
                <div className="text-emerald-400 mb-6 opacity-60">
                  {'>'} Report generated successfully.<br/>
                  {'>'} Outputting final content...
                </div>
                <div className="font-sans text-slate-200 bg-slate-800/50 p-6 rounded-xl border border-slate-700 shadow-inner">
                  <TypingText 
                    text={typeof reportData === 'string' ? reportData : JSON.stringify(reportData, null, 2)} 
                    speed={8} 
                  />
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

// ── CSV Export Section ────────────────────────────────────────────────────────
function CSVExportSection() {
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(false)

  const handleExport = async () => {
    setExporting(true)
    setError(null)
    setSuccess(false)
    try {
      // Dynamic import to avoid circular deps
      const complianceService = await import('../services/complianceService')
      const res = await complianceService.exportCsv()
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'text/csv' }))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `compliance-records-${new Date().toISOString().split('T')[0]}.csv`)
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
      setSuccess(true)
      setTimeout(() => setSuccess(false), 3000)
    } catch (e) {
      setError(e.response?.data?.message || 'Export failed. Please try again.')
    } finally {
      setExporting(false)
    }
  }

  return (
    <div>
      <p className="text-sm text-slate-500 mb-4">Download all compliance records as a CSV file for offline analysis or reporting.</p>
      <button
        id="download-csv-btn"
        onClick={handleExport}
        disabled={exporting}
        className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-blue-600 text-white text-sm font-semibold hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-sm"
      >
        {exporting ? <Spinner size="sm" /> : (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>
        )}
        {exporting ? 'Exporting…' : 'Download CSV'}
      </button>

      {success && (
        <div className="mt-3 flex items-center gap-2 text-sm text-emerald-600 font-medium">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
          CSV downloaded successfully!
        </div>
      )}
      {error && <ErrorAlert message={error} onRetry={handleExport} />}
    </div>
  )
}

// ── Tab Button ────────────────────────────────────────────────────────────────
function TabBtn({ active, onClick, icon, label, id }) {
  return (
    <button
      id={id}
      onClick={onClick}
      className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all ${
        active
          ? 'bg-violet-600 text-white shadow-md shadow-violet-200'
          : 'text-slate-500 hover:text-slate-800 hover:bg-slate-100'
      }`}
    >
      {icon}
      {label}
    </button>
  )
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function AIPanelPage() {
  const [tab, setTab] = useState('ask')        // ask | recommend | report | export
  const [prompt, setPrompt] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [response, setResponse] = useState(null)
  const lastPrompt = useRef('')

  const handleAskAI = useCallback(async (overridePrompt) => {
    const text = overridePrompt ?? prompt
    if (!text.trim()) return
    lastPrompt.current = text
    setLoading(true)
    setError(null)
    setResponse(null)
    try {
      const fn = tab === 'recommend' ? aiService.getRecommendations : aiService.askAI
      const res = await fn(text)
      setResponse(res.data)
    } catch (e) {
      setError(e.response?.data?.error || e.message || 'Could not connect to AI service. Is the Flask server running on port 5000?')
    } finally {
      setLoading(false)
    }
  }, [prompt, tab])

  const handleRetry = () => handleAskAI(lastPrompt.current)

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) handleAskAI()
  }

  const examplePrompts = {
    ask: [
      'What are the key GDPR compliance requirements?',
      'Explain ISO 27001 controls briefly',
      'What is SOC 2 Type II compliance?',
    ],
    recommend: [
      'Our PENDING records are overdue by 30 days',
      'We have 5 NON_COMPLIANT entries in Finance',
      'Recommend steps to improve compliance rate from 60% to 85%',
    ],
  }

  return (
    <div className="page-shell space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <span className="w-8 h-8 bg-violet-600 rounded-xl flex items-center justify-center text-white text-xs font-bold shadow-md">AI</span>
            AI Compliance Assistant
          </h1>
          <p className="page-subtitle">Powered by Groq — ask questions, get recommendations, or generate full reports</p>
        </div>
        <div className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-emerald-50 border border-emerald-200">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          <span className="text-xs font-semibold text-emerald-700">AI Online</span>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex flex-wrap gap-2 p-1.5 bg-slate-100 rounded-2xl w-fit">
        <TabBtn id="tab-ask" active={tab === 'ask'} onClick={() => { setTab('ask'); setResponse(null); setError(null) }}
          label="Ask AI"
          icon={<svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>}
        />
        <TabBtn id="tab-recommend" active={tab === 'recommend'} onClick={() => { setTab('recommend'); setResponse(null); setError(null) }}
          label="Recommendations"
          icon={<svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" /></svg>}
        />
        <TabBtn id="tab-report" active={tab === 'report'} onClick={() => { setTab('report'); setResponse(null); setError(null) }}
          label="Generate Report"
          icon={<svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>}
        />
        <TabBtn id="tab-export" active={tab === 'export'} onClick={() => setTab('export')}
          label="Export CSV"
          icon={<svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg>}
        />
      </div>

      {/* Main Panel */}
      <div className="card">
        {/* Ask AI Tab */}
        {(tab === 'ask' || tab === 'recommend') && (
          <div>
            <h2 className="text-base font-bold text-slate-800 mb-1">
              {tab === 'ask' ? 'Ask the AI Assistant' : 'Get AI Recommendations'}
            </h2>
            <p className="text-sm text-slate-400 mb-4">
              {tab === 'ask'
                ? 'Ask any compliance question and get a structured, detailed response.'
                : 'Describe your compliance situation and receive actionable recommendations.'}
            </p>

            {/* Example prompts */}
            <div className="flex flex-wrap gap-2 mb-4">
              {examplePrompts[tab].map(ex => (
                <button
                  key={ex}
                  onClick={() => { setPrompt(ex); setResponse(null); setError(null) }}
                  className="text-xs px-3 py-1.5 rounded-full border border-violet-200 text-violet-600 bg-violet-50 hover:bg-violet-100 transition-colors"
                >
                  {ex}
                </button>
              ))}
            </div>

            {/* Input */}
            <div className="relative">
              <textarea
                id="ai-prompt-input"
                rows={4}
                placeholder={tab === 'ask' ? 'Ask a compliance question…' : 'Describe your compliance challenge…'}
                value={prompt}
                onChange={e => setPrompt(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={loading}
                className="w-full px-4 py-3 text-sm rounded-xl border border-slate-200 bg-slate-50 outline-none focus:border-violet-400 focus:ring-2 focus:ring-violet-100 focus:bg-white transition resize-none disabled:opacity-60 pr-16"
              />
              <span className="absolute bottom-3 right-3 text-[10px] text-slate-300 select-none">Ctrl+Enter</span>
            </div>

            <div className="flex items-center gap-3 mt-3">
              <button
                id="ask-ai-btn"
                onClick={() => handleAskAI()}
                disabled={!prompt.trim() || loading}
                className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-violet-600 text-white text-sm font-semibold hover:bg-violet-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-md shadow-violet-200"
              >
                {loading ? <Spinner size="sm" /> : (
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                )}
                {loading ? 'Thinking…' : tab === 'ask' ? 'Ask AI' : 'Get Recommendations'}
              </button>
              {(response || error) && (
                <button onClick={() => { setResponse(null); setError(null); setPrompt('') }}
                  className="text-sm text-slate-400 hover:text-slate-600 transition-colors">
                  Clear
                </button>
              )}
            </div>

            {error && <ErrorAlert message={error} onRetry={handleRetry} />}
            <AIResponseCard data={response} loading={loading} onRetry={handleRetry} />
          </div>
        )}

        {/* Report Tab */}
        {tab === 'report' && (
          <div>
            <h2 className="text-base font-bold text-slate-800 mb-1">Streaming AI Report</h2>
            <p className="text-sm text-slate-400 mb-4">
              Submit a report request and watch it generate progressively in real time.
            </p>
            <StreamingReport />
          </div>
        )}

        {/* Export Tab */}
        {tab === 'export' && (
          <div>
            <h2 className="text-base font-bold text-slate-800 mb-1">Export Compliance Data</h2>
            <CSVExportSection />
          </div>
        )}
      </div>

      {/* Info Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {[
          { icon: '⚡', title: 'Instant Answers', desc: 'Get compliance answers powered by Groq LLM in seconds', color: 'from-violet-50 to-purple-50 border-violet-200' },
          { icon: '📊', title: 'Smart Reports', desc: 'Async report generation with real-time progress tracking', color: 'from-blue-50 to-indigo-50 border-blue-200' },
          { icon: '📥', title: 'CSV Export', desc: 'Download all records to spreadsheet with one click', color: 'from-emerald-50 to-teal-50 border-emerald-200' },
        ].map(c => (
          <div key={c.title} className={`rounded-2xl border bg-gradient-to-br ${c.color} p-4`}>
            <p className="text-2xl mb-2">{c.icon}</p>
            <p className="text-sm font-bold text-slate-700">{c.title}</p>
            <p className="text-xs text-slate-500 mt-0.5">{c.desc}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
