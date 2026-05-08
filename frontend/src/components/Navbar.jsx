import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const NAV = [
  { to: '/dashboard',  label: 'Overview'           },
  { to: '/compliance', label: 'Compliance Ledger'  },
  { to: '/tasks',      label: 'Directives'         },
  { to: '/analytics',  label: 'Intelligence'       },
  { to: '/ai',         label: 'Cognitive Hub'      },
]

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="bg-white/70 backdrop-blur-md border-b border-slate-100 sticky top-0 z-30 lg:pl-72 h-20 flex items-center">
      <div className="w-full px-8 lg:px-12">
        <div className="flex items-center justify-between">

          {/* Mobile brand */}
          <div className="flex items-center gap-3 lg:hidden">
            <div className="w-9 h-9 bg-primary-600 rounded-xl flex items-center justify-center shadow-lg shadow-primary-200">
              <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <span className="font-black text-slate-800 text-lg tracking-tighter">ANTIGRAVITY</span>
          </div>

          {/* Page context — desktop */}
          <div className="hidden lg:block">
            <div className="flex items-center gap-3">
               <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
               <p className="text-[10px] font-black text-slate-400 uppercase tracking-[0.3em]">
                 System Online <span className="mx-2 text-slate-200">|</span> 
                 Session Active: <span className="text-slate-800">{user?.name || 'Authorized User'}</span>
               </p>
            </div>
          </div>

          {/* Right actions */}
          <div className="flex items-center gap-6">
            <div className="hidden md:flex flex-col items-end mr-2">
               <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest leading-none mb-1">Authorization Node</p>
               <p className="text-[11px] font-bold text-slate-600 leading-none">{user?.email}</p>
            </div>
            
            <button 
              onClick={handleLogout} 
              className="px-6 py-2.5 bg-slate-50 border border-slate-100 rounded-xl text-[10px] font-black uppercase tracking-widest text-slate-400 hover:text-red-600 hover:border-red-100 hover:bg-red-50 transition-all active:scale-95 shadow-sm"
            >
              Sign Out
            </button>
            
            {/* Mobile toggle */}
            <button
              className="lg:hidden p-3 rounded-xl bg-slate-50 border border-slate-100 text-slate-600 hover:bg-white transition-all"
              onClick={() => setOpen(!open)}
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d={open ? 'M6 18L18 6M6 6l12 12' : 'M4 6h16M4 12h16M4 18h16'} />
              </svg>
            </button>
          </div>
        </div>
      </div>

      {/* Mobile navigation */}
      {open && (
        <div className="absolute top-20 left-0 w-full lg:hidden bg-white/95 backdrop-blur-xl border-b border-slate-100 p-6 space-y-2 animate-in slide-in-from-top duration-300">
          {NAV.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-4 px-6 py-4 rounded-2xl text-[10px] font-black uppercase tracking-widest transition-all ${
                  isActive ? 'bg-slate-900 text-white shadow-xl shadow-slate-200' : 'text-slate-400 hover:bg-slate-50'
                }`
              }
            >
              <div className="w-1.5 h-1.5 rounded-full bg-current opacity-20" />
              {label}
            </NavLink>
          ))}
        </div>
      )}
    </header>
  )
}
