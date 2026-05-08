import api from './api'
import axios from 'axios'

// Base URL for Python AI Flask service (port 5000)
const AI_BASE = import.meta.env.VITE_AI_URL || 'http://localhost:5000'

const aiClient = axios.create({
  baseURL: AI_BASE,
  headers: { 'Content-Type': 'application/json' },
  timeout: 60000, // AI can be slow
})

// ── Describe (Ask AI) ─────────────────────────────────────────────────────────
// POST /describe  { text: "..." }
// Returns: { input, response, generated_at }
export const askAI = (text) =>
  aiClient.post('/describe', { text })

// ── Recommend ─────────────────────────────────────────────────────────────────
// POST /recommend  { text: "..." }
// Returns: { input, recommendations, generated_at }
export const getRecommendations = (text) =>
  aiClient.post('/recommend', { text })

// ── Generate Report (async) ───────────────────────────────────────────────────
// POST /generate-report  { text: "..." }
// Returns: { report_id, status: "PENDING" }
export const generateReport = (text) =>
  aiClient.post('/generate-report', { text })

// ── Poll Report Status ────────────────────────────────────────────────────────
// GET /report/:report_id
// Returns: { status: "PENDING"|"DONE"|"FAILED", data: "...", created_at }
export const getReport = (reportId) =>
  aiClient.get(`/report/${reportId}`)

// ── Java backend AI analysis (if present) ────────────────────────────────────
// POST /api/compliance/:id/analyse
export const getAiAnalysis = (id) =>
  api.post(`/api/compliance/${id}/analyse`)
