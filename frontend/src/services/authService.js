import api from './api'

// Backend: POST /auth/login  →  { token, user: { id, name, email, role } }
export const login = (email, password) =>
  api.post('/auth/login', { email, password })

// Backend: POST /auth/register  →  { message }
export const register = (name, email, password) =>
  api.post('/auth/register', { name, email, password })
