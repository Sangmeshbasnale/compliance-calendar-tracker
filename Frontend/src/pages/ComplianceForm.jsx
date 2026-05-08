import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import * as complianceService from '../services/complianceService'
import {
  STATUS_OPTIONS, PRIORITY_OPTIONS, formatDateForInput,
} from '../utils/helpers'

const EMPTY = { title: '', status: 'PENDING', dueDate: '', priority: 'MEDIUM', description: '' }

function validate(form) {
  const errs = {}
  if (!form.title.trim())   errs.title    = 'Title is required.'
  if (!form.status)         errs.status   = 'Status is required.'
  if (!form.priority)       errs.priority = 'Priority is required.'
  if (!form.dueDate) {
    errs.dueDate = 'Due date is required.'
  } else {
    const d = new Date(form.dueDate)
    if (isNaN(d.getTime())) errs.dueDate = 'Enter a valid date.'
  }
  return errs
}

export default function ComplianceForm() {
  const { id }    = useParams()
  const navigate  = useNavigate()
  const isEdit    = Boolean(id)

  const [form, setForm]       = useState(EMPTY)
  const [errors, setErrors]   = useState({})
  const [loading, setLoading] = useState(false)
  const [fetching, setFetching] = useState(isEdit)
  const [apiError, setApiError] = useState('')

  // Load existing record when editing
  useEffect(() => {
    if (!isEdit) return
    complianceService.getById(id)
      .then(r => {
        const d = r.data
        setForm({
          title:       d.title       ?? '',
          status:      d.status      ?? 'PENDING',
          dueDate:     formatDateForInput(d.dueDate),
          priority:    d.priority    ?? 'MEDIUM',
          description: d.description ?? '',
        })
      })
      .catch(() => setApiError('Failed to load record.'))
      .finally(() => setFetching(false))
  }, [id, isEdit])

  const handleChange = (e) => {
    const { name, value } = e.target
    setForm(f => ({ ...f, [name]: value }))
    if (errors[name]) setErrors(e => ({ ...e, [name]: '' }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errs = validate(form)
    if (Object.keys(errs).length) { setErrors(errs); return }

    setLoading(true)
    setApiError('')
    try {
      if (isEdit) {
        await complianceService.update(id, form)
      } else {
        await complianceService.create(form)
      }
      navigate('/compliance')
    } catch (err) {
      setApiError(err.response?.data?.message || 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  if (fetching) {
    return (
      <div className="max-w-2xl mx-auto space-y-4 animate-pulse">
        <div className="h-8 bg-slate-200 rounded-xl w-48" />
        <div className="card space-y-4">
          {[1,2,3,4,5].map(i => <div key={i} className="h-10 bg-slate-100 rounded-xl" />)}
        </div>
      </div>
    )
  }

  return (
    <div className="page-shell max-w-3xl mx-auto">
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate('/compliance')} className="btn-secondary p-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <div>
          <h1 className="page-title">{isEdit ? 'Edit Record' : 'New Record'}</h1>
          <p className="page-subtitle">{isEdit ? 'Update compliance record details' : 'Create a new compliance record'}</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} noValidate>
        <div className="card space-y-5">

          {/* API Error */}
          {apiError && (
            <div className="flex items-start gap-3 bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3 rounded-xl">
              <svg className="w-4 h-4 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {apiError}
            </div>
          )}

          {/* Title */}
          <div>
            <label className="form-label" htmlFor="title">
              Title <span className="text-red-500">*</span>
            </label>
            <input
              id="title"
              name="title"
              type="text"
              className={`form-input ${errors.title ? 'border-red-400 focus:border-red-400 focus:ring-red-100' : ''}`}
              placeholder="e.g. GDPR Data Audit Q3"
              value={form.title}
              onChange={handleChange}
            />
            {errors.title && (
              <p className="form-error">
                <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
                {errors.title}
              </p>
            )}
          </div>

          {/* Description */}
          <div>
            <label className="form-label" htmlFor="description">Description</label>
            <textarea
              id="description"
              name="description"
              rows={3}
              className="form-input resize-none"
              placeholder="Optional details about this compliance record..."
              value={form.description}
              onChange={handleChange}
            />
          </div>

          {/* Status + Priority */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="form-label" htmlFor="status">
                Status <span className="text-red-500">*</span>
              </label>
              <select
                id="status"
                name="status"
                className={`form-select ${errors.status ? 'border-red-400' : ''}`}
                value={form.status}
                onChange={handleChange}
              >
                {STATUS_OPTIONS.map(o => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
              {errors.status && <p className="form-error">{errors.status}</p>}
            </div>

            <div>
              <label className="form-label" htmlFor="priority">
                Priority <span className="text-red-500">*</span>
              </label>
              <select
                id="priority"
                name="priority"
                className={`form-select ${errors.priority ? 'border-red-400' : ''}`}
                value={form.priority}
                onChange={handleChange}
              >
                {PRIORITY_OPTIONS.map(o => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
              {errors.priority && <p className="form-error">{errors.priority}</p>}
            </div>
          </div>

          {/* Due Date */}
          <div>
            <label className="form-label" htmlFor="dueDate">
              Due Date <span className="text-red-500">*</span>
            </label>
            <input
              id="dueDate"
              name="dueDate"
              type="date"
              className={`form-input ${errors.dueDate ? 'border-red-400 focus:border-red-400 focus:ring-red-100' : ''}`}
              value={form.dueDate}
              onChange={handleChange}
            />
            {errors.dueDate && (
              <p className="form-error">
                <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
                {errors.dueDate}
              </p>
            )}
          </div>

          {/* Actions */}
          <div className="flex items-center gap-3 pt-2">
            <button type="submit" className="btn-primary flex-1" disabled={loading}>
              {loading ? (
                <>
                  <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                  </svg>
                  {isEdit ? 'Saving...' : 'Creating...'}
                </>
              ) : (
                isEdit ? 'Save Changes' : 'Create Record'
              )}
            </button>
            <button
              type="button"
              onClick={() => navigate('/compliance')}
              className="btn-secondary flex-1"
              disabled={loading}
            >
              Cancel
            </button>
          </div>
        </div>
      </form>
    </div>
  )
}
