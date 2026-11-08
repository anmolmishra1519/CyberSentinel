import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
})

// Normalizes axios/network errors into a single shape the UI can render
// consistently (ErrorState component), instead of letting raw errors leak
// into the UI or failing silently.
function normalizeError(error) {
  if (error.response) {
    const data = error.response.data || {}
    return {
      status: error.response.status,
      message: data.message || 'The server returned an error. Please try again.',
    }
  }
  if (error.request) {
    return {
      status: 0,
      message: 'Could not reach the backend API. Is Spring Boot running on port 8080?',
    }
  }
  return { status: -1, message: error.message || 'Unexpected error.' }
}

async function request(fn) {
  try {
    const res = await fn()
    return res.data
  } catch (err) {
    throw normalizeError(err)
  }
}

export const api = {
  getDashboardStats: () => request(() => client.get('/dashboard/stats')),

  getThreats: (params) => request(() => client.get('/threats', { params })),
  getThreat: (id) => request(() => client.get(`/threats/${id}`)),

  getAlerts: (openOnly = false) => request(() => client.get('/alerts', { params: { openOnly } })),

  validateCsv: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request(() => client.post('/traffic/validate', form))
  },
  analyzeCsv: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request(() => client.post('/traffic/analyze', form, { timeout: 60000 }))
  },

  startSimulation: (attackType) =>
    request(() => client.post('/simulation/start', { attackType })),
  getSimulationStatus: () => request(() => client.get('/simulation/status')),

  getSystemStatus: () => request(() => client.get('/system/status')),
}

export default api
