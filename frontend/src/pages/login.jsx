import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import toast from 'react-hot-toast'

export default function Login() {
  const { login }  = useAuth()
  const navigate   = useNavigate()
  const [email,    setEmail]    = useState('')
  const [password, setPassword] = useState('')
  const [loading,  setLoading]  = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!email || !password) {
      toast.error('Identity credentials required')
      return
    }

    setLoading(true)
    const loadToast = toast.loading('Authenticating credentials...')
    try {
      await login(email, password)
      toast.success('Access granted', { id: loadToast })
      navigate('/dashboard')
    } catch (err) {
      const data = err.response?.data
      const apiMessage = typeof data === 'object'
        ? data?.message || Object.values(data?.errors ?? {})[0]
        : data
      toast.error(apiMessage || 'Protocol rejection: Invalid credentials', { id: loadToast })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-[#f8fafc] flex items-center justify-center p-6 animate-in fade-in duration-1000">
      <div className="w-full max-w-xl">
        <div className="bg-white rounded-[3rem] border border-slate-100 p-12 sm:p-16 shadow-[0_35px_60px_-15px_rgba(0,0,0,0.03)] relative overflow-hidden group">
          {/* Decorative Elements */}
          <div className="absolute top-0 right-0 w-64 h-64 bg-primary-50 rounded-bl-full opacity-30 -mr-20 -mt-20 transition-all group-hover:scale-110" />
          
          <div className="relative text-center mb-12">
            <div className="mx-auto mb-8 flex h-20 w-20 items-center justify-center rounded-[1.75rem] bg-primary-600 shadow-2xl shadow-primary-200 rotate-3 group-hover:rotate-6 transition-transform">
              <span className="text-4xl font-black text-white italic">C</span>
            </div>
            <h1 className="text-4xl font-black text-slate-800 tracking-tighter">Core Authorization</h1>
            <p className="text-[10px] font-black text-slate-400 uppercase tracking-[0.3em] mt-2">Compliance Management Ledger</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-8 relative" noValidate>
            <div className="space-y-2">
              <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-4" htmlFor="email">Email Identifier</label>
              <input
                id="email"
                type="email"
                className="w-full px-8 py-5 rounded-3xl border-2 border-slate-50 bg-slate-50 outline-none focus:border-primary-400 focus:ring-8 focus:ring-primary-50 focus:bg-white transition-all font-bold placeholder:text-slate-300"
                placeholder="identity@corporation.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
                autoComplete="email"
              />
            </div>

            <div className="space-y-2">
              <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-4" htmlFor="password">Passcode</label>
              <input
                id="password"
                type="password"
                className="w-full px-8 py-5 rounded-3xl border-2 border-slate-50 bg-slate-50 outline-none focus:border-primary-400 focus:ring-8 focus:ring-primary-50 focus:bg-white transition-all font-bold placeholder:text-slate-300"
                placeholder="••••••••"
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
                autoComplete="current-password"
              />
            </div>

            <button type="submit" className="w-full py-6 rounded-[1.5rem] bg-slate-900 text-white text-xs font-black uppercase tracking-[0.2em] shadow-2xl shadow-slate-200 hover:bg-primary-600 hover:shadow-primary-100 transition-all flex items-center justify-center gap-3 active:scale-95 disabled:opacity-50" disabled={loading}>
              {loading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Processing Protocol
                </>
              ) : 'Execute Log-In Sequence'}
            </button>
          </form>

          <div className="mt-12 text-center relative border-t border-slate-50 pt-8">
            <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">
              Awaiting credentials?{' '}
              <Link to="/register" className="text-primary-600 hover:text-primary-700 transition-colors">
                Initialize Account
              </Link>
            </p>
          </div>
        </div>

        {/* Footer info */}
        <div className="mt-10 flex items-center justify-center gap-8 opacity-40">
           {['Security Node 1', 'SSL Encrypted', 'Auth v4.0'].map(inf => (
             <span key={inf} className="text-[9px] font-black uppercase tracking-[0.2em] text-slate-500">{inf}</span>
           ))}
        </div>
      </div>
    </div>
  )
}

