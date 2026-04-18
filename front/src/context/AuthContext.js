import { createContext, useState, useContext, useEffect } from 'react'
import authService from '../services/authService'

const AuthContext = createContext(null)

// Helper functions to get the appropriate storage
const getStorage = (rememberMe) => rememberMe ? localStorage : sessionStorage

const getStoredData = (key) => {
  // Check localStorage first, then sessionStorage
  return localStorage.getItem(key) || sessionStorage.getItem(key)
}

const clearAllStorage = () => {
  // Clear from both storages
  localStorage.removeItem('user')
  localStorage.removeItem('token')
  sessionStorage.removeItem('user')
  sessionStorage.removeItem('token')
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Check both localStorage and sessionStorage for existing session
    const storedUser = getStoredData('user')
    if (storedUser) {
      setUser(JSON.parse(storedUser))
    }
    setLoading(false)
  }, [])

  const login = async (username, password, rememberMe = false) => {
    const response = await authService.login(username, password)
    setUser(response)
    
    const storage = getStorage(rememberMe)
    storage.setItem('user', JSON.stringify(response))
    storage.setItem('token', response.token)
    
    return response
  }

  const signup = async (userData) => {
    return await authService.signup(userData)
  }

  const logout = () => {
    setUser(null)
    clearAllStorage()
  }

  if (loading) {
    return <div>Loading...</div>
  }

  return (
    <AuthContext.Provider value={{ user, login, signup, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
