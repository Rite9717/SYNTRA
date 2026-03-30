import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import Navbar from '../components/Navbar';
import './AdminDashboard.css';

const AdminDashboard = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    fetchDashboardStats();
  }, []);

  const fetchDashboardStats = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${import.meta.env.REACT_APP_API_URL}/api/admin/dashboard/stats`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setStats(response.data);
      setLoading(false);
    } catch (error) {
      console.error('Error fetching dashboard stats:', error);
      if (error.response?.status === 403) {
        alert('Access denied. Admin privileges required.');
        navigate('/inbox');
      }
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loading">Loading dashboard...</div>;
  }

  return (
    <>
      <Navbar />
      <div className="admin-dashboard">
        <div className="dashboard-header">
        <h1>Admin Dashboard</h1>
        <button onClick={() => navigate('/admin/users')} className="btn-primary">
          Manage Users
        </button>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon">👥</div>
          <div className="stat-content">
            <h3>Total Users</h3>
            <p className="stat-number">{stats?.totalUsers || 0}</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon">✅</div>
          <div className="stat-content">
            <h3>Active Users</h3>
            <p className="stat-number">{stats?.activeUsers || 0}</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon">📧</div>
          <div className="stat-content">
            <h3>Total Messages</h3>
            <p className="stat-number">{stats?.totalMessages || 0}</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon">📅</div>
          <div className="stat-content">
            <h3>Messages Today</h3>
            <p className="stat-number">{stats?.messagesToday || 0}</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon">📬</div>
          <div className="stat-content">
            <h3>Unread Messages</h3>
            <p className="stat-number">{stats?.unreadMessages || 0}</p>
          </div>
        </div>
      </div>

      <div className="dashboard-actions">
        <button onClick={() => navigate('/admin/users')} className="action-btn">
          <span>👥</span>
          <span>User Management</span>
        </button>
        <button onClick={() => navigate('/admin/create-user')} className="action-btn">
          <span>➕</span>
          <span>Create User</span>
        </button>
        <button onClick={() => navigate('/inbox')} className="action-btn">
          <span>📧</span>
          <span>View Messages</span>
        </button>
        <button onClick={fetchDashboardStats} className="action-btn">
          <span>🔄</span>
          <span>Refresh Stats</span>
        </button>
      </div>
      </div>
    </>
  );
};

export default AdminDashboard;
