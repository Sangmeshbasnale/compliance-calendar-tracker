import { Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import { useAuth } from './context/AuthContext'
import Sidebar from './components/Sidebar'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import ErrorBoundary from './components/ErrorBoundary'

import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import ComplianceList from './pages/ComplianceList'
import ComplianceDetail from './pages/ComplianceDetail'
import ComplianceForm from './pages/ComplianceForm'
import Analytics from './pages/Analytics'
import TaskList from './pages/TaskList'
import AIPanelPage from './pages/AIPanelPage'

function AppShell({ children }) {
  return (
    <div className="flex min-h-screen bg-slate-50">
      <Sidebar />
      <div className="flex-1 lg:pl-60 flex flex-col min-h-screen">
        <Navbar />
        <main className="flex-1 pt-16">
          <ErrorBoundary>
            {children}
          </ErrorBoundary>
        </main>
      </div>
    </div>
  )
}

export default function App() {
  const { isAuthenticated } = useAuth()

  return (
    <>
      <Toaster 
        position="top-right"
        toastOptions={{
          duration: 4000,
          style: {
            background: '#ffffff',
            color: '#0f172a',
            borderRadius: '16px',
            boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
            border: '1px solid #f1f5f9',
            padding: '16px 20px',
            fontSize: '14px',
            fontWeight: '500',
          },
          success: {
            iconTheme: {
              primary: '#8b5cf6',
              secondary: '#ffffff',
            },
          },
        }}
      />
      <Routes>
        {/* Public */}
        <Route path="/login"    element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* Protected */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />

        <Route path="/dashboard" element={
          <ProtectedRoute>
            <AppShell><Dashboard /></AppShell>
          </ProtectedRoute>
        } />

        <Route path="/compliance" element={
          <ProtectedRoute>
            <AppShell><ComplianceList /></AppShell>
          </ProtectedRoute>
        } />

        <Route path="/compliance/new" element={
          <ProtectedRoute>
            <AppShell><ComplianceForm /></AppShell>
          </ProtectedRoute>
        } />

        <Route path="/compliance/:id" element={
          <ProtectedRoute>
            <AppShell><ComplianceDetail /></AppShell>
          </ProtectedRoute>
        } />

        <Route path="/compliance/:id/edit" element={
          <ProtectedRoute>
            <AppShell><ComplianceForm /></AppShell>
          </ProtectedRoute>
        } />

        <Route path="/analytics" element={
          <ProtectedRoute>
            <AppShell><Analytics /></AppShell>
          </ProtectedRoute>
        } />

        <Route path="/tasks" element={
          <ProtectedRoute>
            <AppShell><TaskList /></AppShell>
          </ProtectedRoute>
        } />

        <Route path="/ai" element={
          <ProtectedRoute>
            <AppShell><AIPanelPage /></AppShell>
          </ProtectedRoute>
        } />

        {/* Fallback */}
        <Route path="*" element={<Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />} />
      </Routes>
    </>
  )
}
