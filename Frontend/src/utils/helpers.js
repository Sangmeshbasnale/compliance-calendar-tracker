// ── Date helpers ──────────────────────────────────────────────────────────────

export function formatDate(dateStr) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric',
  })
}

export function isOverdue(dateStr) {
  if (!dateStr) return false
  const d = new Date(dateStr)
  d.setHours(0, 0, 0, 0)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return d < today
}

export function formatDateForInput(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toISOString().split('T')[0]
}

// ── Status / Priority maps ────────────────────────────────────────────────────

export const STATUS_OPTIONS = [
  { value: 'COMPLIANT',     label: 'Compliant'     },
  { value: 'NON_COMPLIANT', label: 'Non-Compliant' },
  { value: 'PENDING',       label: 'Pending'       },
  { value: 'IN_PROGRESS',   label: 'In Progress'   },
]

export const PRIORITY_OPTIONS = [
  { value: 'HIGH',   label: 'High'   },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'LOW',    label: 'Low'    },
]

export const STATUS_MAP = {
  COMPLIANT:     { label: 'Compliant',     cls: 'bg-emerald-100 text-emerald-800 ring-emerald-200' },
  NON_COMPLIANT: { label: 'Non-Compliant', cls: 'bg-red-100 text-red-800 ring-red-200'             },
  PENDING:       { label: 'Pending',       cls: 'bg-amber-100 text-amber-800 ring-amber-200'       },
  IN_PROGRESS:   { label: 'In Progress',   cls: 'bg-blue-100 text-blue-800 ring-blue-200'          },
}

export const PRIORITY_MAP = {
  HIGH:   { label: 'High',   cls: 'bg-red-100 text-red-700 ring-red-200'            },
  MEDIUM: { label: 'Medium', cls: 'bg-amber-100 text-amber-700 ring-amber-200'      },
  LOW:    { label: 'Low',    cls: 'bg-emerald-100 text-emerald-700 ring-emerald-200' },
}
