import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth'
import Shell from './layout/Shell'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Inbox from './pages/Inbox'
import Compose from './pages/Compose'
import Chat from './pages/Chat'
import Notifications from './pages/Notifications'
import Admin from './pages/Admin'
import Users from './pages/Users'

function Private({ children }) {
  const { user } = useAuth()
  return user ? children : <Navigate to="/login" />
}

function AdminOnly({ children }) {
  const { user, isAdmin } = useAuth()
  if (!user) return <Navigate to="/login" />
  return isAdmin ? children : <Navigate to="/dashboard" />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<Private><Shell /></Private>}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/inbox" element={<Inbox />} />
        <Route path="/inbox/:id" element={<Inbox />} />
        <Route path="/compose" element={<Compose />} />
        <Route path="/chat" element={<Chat />} />
        <Route path="/chat/:roomId" element={<Chat />} />
        <Route path="/notifications" element={<Notifications />} />
        <Route path="/admin" element={<AdminOnly><Admin /></AdminOnly>} />
        <Route path="/admin/users" element={<AdminOnly><Users /></AdminOnly>} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" />} />
    </Routes>
  )
}
