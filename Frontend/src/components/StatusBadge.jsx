import { STATUS_MAP, PRIORITY_MAP } from '../utils/helpers'

export function StatusBadge({ status }) {
  const { label, cls } = STATUS_MAP[status] ?? {
    label: status ?? '—',
    cls: 'bg-slate-100 text-slate-600 ring-slate-200',
  }
  return (
    <span className={`badge ${cls}`}>
      <span className="w-1.5 h-1.5 rounded-full bg-current opacity-70" />
      {label}
    </span>
  )
}

export function PriorityBadge({ priority }) {
  const { label, cls } = PRIORITY_MAP[priority?.toUpperCase()] ?? {
    label: priority ?? '—',
    cls: 'bg-slate-100 text-slate-600 ring-slate-200',
  }
  return <span className={`badge ${cls}`}>{label}</span>
}
