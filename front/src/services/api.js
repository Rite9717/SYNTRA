import axios from 'axios'

const API_URL = `${process.env.REACT_APP_API_URL}/api`

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Helper to get token from either storage
const getToken = () => {
  return localStorage.getItem('token') || sessionStorage.getItem('token')
}

// Helper to clear all storage
const clearAllStorage = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('user')
}

api.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAllStorage()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
