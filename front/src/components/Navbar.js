import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import './Navbar.css'

function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const isAdmin = user?.roles?.includes('ROLE_ADMIN')

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to={isAdmin ? "/admin/dashboard" : "/dashboard"} className="navbar-brand">
          IMS - College Mailing System
        </Link>
        <div className="navbar-menu">
          {isAdmin ? (
            <>
              <Link to="/admin/dashboard" className="navbar-link">Admin Dashboard</Link>
              <Link to="/admin/users" className="navbar-link">User Management</Link>
              <Link to="/inbox" className="navbar-link">Messages</Link>
              <Link to="/chat" className="navbar-link">Chat</Link>
            </>
          ) : (
            <>
              <Link to="/dashboard" className="navbar-link">Dashboard</Link>
              <Link to="/inbox" className="navbar-link">Inbox</Link>
              <Link to="/compose" className="navbar-link">Compose</Link>
              <Link to="/sent" className="navbar-link">Sent</Link>
              <Link to="/chat" className="navbar-link">Chat</Link>
            </>
          )}
          <div className="navbar-user">
            <span>Welcome, {user?.username} {isAdmin && '(Admin)'}</span>
            <button onClick={handleLogout} className="btn btn-secondary">
              Logout
            </button>
          </div>
        </div>
      </div>
    </nav>
  )
}

export default Navbar
