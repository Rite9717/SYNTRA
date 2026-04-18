import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Inbox from './pages/Inbox'
import Compose from './pages/Compose'
import SentMessages from './pages/SentMessages'
import AdminDashboard from './pages/AdminDashboard'
import UserManagement from './pages/UserManagement'
import Chat from './pages/Chat'
import CreateUser from './pages/CreateUser'

function PrivateRoute({ children }) {
  const { user } = useAuth()
  return user ? children : <Navigate to="/login" />
}

function AdminRoute({ children }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" />
  if (!user.roles?.includes('ROLE_ADMIN')) return <Navigate to="/dashboard" />
  return children
}

function App() {
  console.log("API:", process.env.REACT_APP_API_URL);
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          {/* /signup route completely removed */}

          <Route path="/dashboard" element={
            <PrivateRoute><Dashboard /></PrivateRoute>
          } />
          <Route path="/inbox" element={
            <PrivateRoute><Inbox /></PrivateRoute>
          } />
          <Route path="/compose" element={
            <PrivateRoute><Compose /></PrivateRoute>
          } />
          <Route path="/sent" element={
            <PrivateRoute><SentMessages /></PrivateRoute>
          } />
          <Route path="/chat" element={
            <PrivateRoute><Chat /></PrivateRoute>
          } />

          {/* Admin only routes */}
          <Route path="/admin/dashboard" element={
            <AdminRoute><AdminDashboard /></AdminRoute>
          } />
          <Route path="/admin/users" element={
            <AdminRoute><UserManagement /></AdminRoute>
          } />
          <Route path="/admin/create-user" element={
            <AdminRoute><CreateUser /></AdminRoute>
          } />

          <Route path="/" element={<Navigate to="/dashboard" />} />
        </Routes>
      </Router>
    </AuthProvider>
  )
}

export default App