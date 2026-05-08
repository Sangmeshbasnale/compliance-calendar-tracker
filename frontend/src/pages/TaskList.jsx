import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../services/api'
import { StatusBadge, PriorityBadge } from '../components/StatusBadge'
import { formatDate, isOverdue } from '../utils/helpers'
import LoadingSkeleton from '../components/LoadingSkeleton'
import EmptyState from '../components/EmptyState'

export default function TaskList() {
  const navigate = useNavigate()
  const [tasks,   setTasks]   = useState([])
  const [loading, setLoading] = useState(true)
  const [error,   setError]   = useState(null)

  const loadTasks = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await api.get('/api/compliance')
      const data = res.data?.content ?? (Array.isArray(res.data) ? res.data : [])
      setTasks(data)
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to fetch tasks.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadTasks() }, [loadTasks])

  const stats = {
    total:      tasks.length,
    completed:  tasks.filter(t => t.status === 'COMPLIANT').length,
    inProgress: tasks.filter(t => t.status === 'IN_PROGRESS').length,
    overdue:    tasks.filter(t => isOverdue(t.dueDate) && t.status !== 'COMPLIANT').length,
  }

  const kpis = [
    { label: 'Total Managed',       value: stats.total,      color: 'bg-indigo-50 text-indigo-600', icon: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10'  },
    { label: 'Compliant',   value: stats.completed,  color: 'bg-emerald-50 text-emerald-600', icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z' },
    { label: 'In Progress', value: stats.inProgress, color: 'bg-blue-50 text-blue-600', icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z'   },
    { label: 'Overdue',     value: stats.overdue,    color: 'bg-red-50 text-red-600', icon: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z'    },
  ]

  if (error) {
    return (
      <div className="page-shell max-w-2xl mx-auto flex flex-col items-center justify-center min-h-[60vh]">
        <EmptyState 
          title="Unable to Load Tasks"
          message={error}
          icon={
            <svg className="w-14 h-14 text-red-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          }
          action={{ label: "Retry Connection", onClick: loadTasks }}
        />
      </div>
    )
  }

  return (
    <div className="page-shell space-y-8 max-w-[1400px] mx-auto animate-in fade-in slide-in-from-bottom-4 duration-700">

      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 border-b border-slate-100 pb-8">
        <div>
          <h1 className="text-4xl font-black text-slate-800 tracking-tighter">Compliance Ledger</h1>
          <p className="text-sm font-bold text-slate-400 uppercase tracking-[0.2em] mt-1">Audit log & task management system</p>
        </div>
        <button onClick={() => navigate('/compliance/new')} className="btn-primary rounded-2xl px-8 py-4 font-black uppercase tracking-widest shadow-xl shadow-primary-100 hover:scale-105 active:scale-95 transition-all">
          + Add New Entry
        </button>
      </div>

      {/* KPI Cards */}
      {loading ? (
        <LoadingSkeleton type="grid" />
      ) : (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-6">
          {kpis.map(({ label, value, color, icon }) => (
            <div key={label} className="bg-white rounded-3xl border border-slate-100 p-8 shadow-sm hover:shadow-md transition-all group">
              <div className={`w-12 h-12 rounded-2xl flex items-center justify-center mb-6 ${color} group-hover:scale-110 transition-transform`}>
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d={icon} /></svg>
              </div>
              <p className="text-4xl font-black text-slate-800 tracking-tighter mb-1">{value}</p>
              <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">{label}</p>
            </div>
          ))}
        </div>
      )}

      {/* Table Container */}
      <div className="bg-white rounded-[2.5rem] border border-slate-100 overflow-hidden shadow-sm">
        <div className="px-10 py-8 border-b border-slate-50 flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-black text-slate-800 tracking-tight">Active Workload</h2>
            <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mt-1">Status indexed documentation</p>
          </div>
          {!loading && tasks.length > 0 && (
            <span className="px-4 py-2 bg-slate-50 rounded-xl text-[10px] font-black text-slate-500 uppercase tracking-widest border border-slate-100">
              {tasks.length} Total Records
            </span>
          )}
        </div>

        {loading ? (
          <div className="p-4">
             <LoadingSkeleton type="list" />
          </div>
        ) : tasks.length === 0 ? (
          <div className="py-24">
            <EmptyState 
              title="No documentation found"
              message="Your compliance ledger is currently empty. Start by adding your first audit or task record."
              action={{ label: "Initialize First Record", onClick: () => navigate('/compliance/new') }}
            />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px]">
              <thead className="bg-slate-50/50">
                <tr>
                  {['Compliance Item', 'Status', 'Deadline', 'Criticality', 'Management'].map(h => (
                    <th key={h} className={`px-10 py-5 text-[10px] font-black text-slate-400 uppercase tracking-[0.2em] ${h === 'Management' ? 'text-right' : 'text-left'}`}>
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {tasks.map((task, idx) => {
                  const overdue = isOverdue(task.dueDate) && task.status !== 'COMPLIANT'
                  return (
                    <tr
                      key={task.id}
                      onClick={() => navigate(`/compliance/${task.id}`)}
                      className="hover:bg-slate-50/80 cursor-pointer transition-all group animate-in fade-in slide-in-from-left duration-500"
                      style={{ animationDelay: `${idx * 50}ms` }}
                    >
                      <td className="px-10 py-7">
                        <div className="flex items-center gap-4">
                          <div className="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center text-[10px] font-black text-slate-400 transition-colors group-hover:bg-white group-hover:text-primary-600">
                            #{task.id}
                          </div>
                          <div>
                            <p className="text-base font-black text-slate-800 tracking-tight group-hover:text-primary-600 transition-colors">{task.title || 'Untitled Entry'}</p>
                            {task.description && (
                              <p className="text-xs text-slate-400 font-bold uppercase tracking-wider mt-0.5 truncate max-w-[300px]">{task.description}</p>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="px-10 py-7">
                        <StatusBadge status={task.status} />
                      </td>
                      <td className="px-10 py-7">
                        <div className="flex flex-col">
                           <span className={`text-sm font-bold ${overdue ? 'text-red-500' : 'text-slate-600'}`}>
                             {formatDate(task.dueDate)}
                           </span>
                           {overdue && <span className="text-[10px] font-black text-red-400 uppercase tracking-widest mt-1 animate-pulse">Critical Overdue</span>}
                        </div>
                      </td>
                      <td className="px-10 py-7">
                        <PriorityBadge priority={task.priority} />
                      </td>
                      <td className="px-10 py-7" onClick={e => e.stopPropagation()}>
                        <div className="flex items-center justify-end gap-3">
                          <button
                            onClick={() => navigate(`/compliance/${task.id}/edit`)}
                            className="p-3 text-slate-400 hover:text-primary-600 hover:bg-white rounded-2xl transition-all border border-transparent hover:border-slate-100 shadow-sm hover:shadow-md"
                            title="Edit"
                          >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                                d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                            </svg>
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        {!loading && tasks.length > 0 && (
          <div className="px-10 py-6 border-t border-slate-50 bg-slate-50/30">
            <p className="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em]">
              Showing <span className="text-slate-800">{tasks.length}</span> Active Tasks
              {' · '}{new Date().toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
