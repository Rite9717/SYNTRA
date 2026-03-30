import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import Navbar from '../components/Navbar'
import messageService from '../services/messageService'
import './Dashboard.css'

function Dashboard() {
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(true)
  const { user } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    // Redirect admin users to admin dashboard
    if (user && user.roles && user.roles.includes('ROLE_ADMIN')) {
      navigate('/admin/dashboard')
      return
    }
    loadUnreadCount()
  }, [user, navigate])

  const loadUnreadCount = async () => {
    try {
      const count = await messageService.getUnreadCount()
      setUnreadCount(count)
    } catch (error) {
      console.error('Error loading unread count:', error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <Navbar />
      <div className="container">
        <div className="dashboard">
          <h1>Welcome to IMS</h1>
          <p className="subtitle">College Mailing and Chatting System</p>
          
          <div className="dashboard-grid">
            <Link to="/inbox" className="dashboard-card">
              <div className="card-icon">📥</div>
              <h3>Inbox</h3>
              {!loading && unreadCount > 0 && (
                <span className="badge">{unreadCount} unread</span>
              )}
              <p>View your received messages</p>
            </Link>

            <Link to="/compose" className="dashboard-card">
              <div className="card-icon">✉️</div>
              <h3>Compose</h3>
              <p>Send a new message</p>
            </Link>

            <Link to="/sent" className="dashboard-card">
              <div className="card-icon">📤</div>
              <h3>Sent Messages</h3>
              <p>View messages you've sent</p>
            </Link>

            <div className="dashboard-card">
              <div className="card-icon">💬</div>
              <h3>Chat</h3>
              <p>Coming soon...</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Dashboard
