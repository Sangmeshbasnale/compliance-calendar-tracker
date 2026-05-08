import api from './api'

// GET /api/compliance — supports page, size, sortBy, sortDir, status, priority, search, dateFrom, dateTo
export const getAll = (params = {}) =>
  api.get('/api/compliance', { params })

// GET /api/compliance/{id}
export const getById = (id) =>
  api.get(`/api/compliance/${id}`)

// POST /api/compliance
export const create = (data) =>
  api.post('/api/compliance', data)

// PUT /api/compliance/{id}
export const update = (id, data) =>
  api.put(`/api/compliance/${id}`, data)

// DELETE /api/compliance/{id}
export const remove = (id) =>
  api.delete(`/api/compliance/${id}`)

// GET /api/compliance/search?q=keyword
export const search = (query) =>
  api.get('/api/compliance/search', { params: { q: query } })

// GET /api/compliance/stats
export const getStats = () =>
  api.get('/api/compliance/stats')

// GET /api/compliance/export — returns blob for CSV download
export const exportCsv = () =>
  api.get('/api/compliance/export', { responseType: 'blob' })

// POST /api/compliance/{id}/analyse — AI analysis
export const getAiAnalysis = (id) =>
  api.post(`/api/compliance/${id}/analyse`)
