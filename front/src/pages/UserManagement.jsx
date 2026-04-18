import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import Navbar from '../components/Navbar';
import './UserManagement.css';

const UserManagement = () => {
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [selectedUser, setSelectedUser] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchUsers();
  }, []);

  useEffect(() => {
    if (searchTerm) {
      const filtered = users.filter(user =>
        user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
        user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
        user.firstName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        user.lastName?.toLowerCase().includes(searchTerm.toLowerCase())
      );
      setFilteredUsers(filtered);
    } else {
      setFilteredUsers(users);
    }
  }, [searchTerm, users]);

  const fetchUsers = async () => {
    try {
      const token = sessionStorage.getItem('token');
      const response = await axios.get(`${process.env.REACT_APP_API_URL}/api/admin/users`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setUsers(response.data);
      setFilteredUsers(response.data);
      setLoading(false);
    } catch (error) {
      console.error('Error fetching users:', error);
      if (error.response?.status === 403) {
        alert('Access denied. Admin privileges required.');
        navigate('/inbox');
      }
      setLoading(false);
    }
  };

  const toggleUserStatus = async (userId) => {
    try {
      const token = sessionStorage.getItem('token');
      await axios.put(`${process.env.REACT_APP_API_URL}/api/admin/users/${userId}/toggle-status`,
        {},
        { headers: { Authorization: `Bearer ${token}` } }
      );
      fetchUsers();
      alert('User status updated successfully');
    } catch (error) {
      console.error('Error toggling user status:', error);
      alert('Failed to update user status');
    }
  };

  const viewUserDetails = (user) => {
    setSelectedUser(user);
  };

  const closeModal = () => {
    setSelectedUser(null);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Never';
    return new Date(dateString).toLocaleString();
  };

  if (loading) {
    return <div className="loading">Loading users...</div>;
  }

  return (
    <>
      <Navbar />
      <div className="user-management">
        <div className="management-header">
        <h1>User Management</h1>
        <button onClick={() => navigate('/admin/dashboard')} className="btn-secondary">
          Back to Dashboard
        </button>
      </div>

      <div className="search-bar">
        <input
          type="text"
          placeholder="Search users by username, email, or name..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="search-input"
        />
        <span className="search-icon">🔍</span>
      </div>

      <div className="users-stats">
        <div className="stat-item">
          <span>Total Users:</span>
          <strong>{users.length}</strong>
        </div>
        <div className="stat-item">
          <span>Active Users:</span>
          <strong>{users.filter(u => u.active).length}</strong>
        </div>
        <div className="stat-item">
          <span>Inactive Users:</span>
          <strong>{users.filter(u => !u.active).length}</strong>
        </div>
      </div>

      <div className="users-table-container">
        <table className="users-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Email</th>
              <th>Name</th>
              <th>Status</th>
              <th>Roles</th>
              <th>Messages</th>
              <th>Last Login</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.map(user => (
              <tr key={user.id} className={!user.active ? 'inactive-user' : ''}>
                <td>{user.id}</td>
                <td>{user.username}</td>
                <td>{user.email}</td>
                <td>{user.firstName} {user.lastName}</td>
                <td>
                  <span className={`status-badge ${user.active ? 'active' : 'inactive'}`}>
                    {user.active ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td>
                  <div className="roles">
                    {user.roles?.map(role => (
                      <span key={role} className="role-badge">{role}</span>
                    ))}
                  </div>
                </td>
                <td>
                  <div className="message-count">
                    <span>📤 {user.sentMessagesCount}</span>
                    <span>📥 {user.receivedMessagesCount}</span>
                  </div>
                </td>
                <td>{formatDate(user.lastLogin)}</td>
                <td>
                  <div className="action-buttons">
                    <button
                      onClick={() => viewUserDetails(user)}
                      className="btn-view"
                      title="View Details"
                    >
                      👁️
                    </button>
                    <button
                      onClick={() => toggleUserStatus(user.id)}
                      className={`btn-toggle ${user.active ? 'deactivate' : 'activate'}`}
                      title={user.active ? 'Deactivate' : 'Activate'}
                    >
                      {user.active ? '🔒' : '🔓'}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selectedUser && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>User Details</h2>
              <button onClick={closeModal} className="close-btn">×</button>
            </div>
            <div className="modal-body">
              <div className="detail-row">
                <span className="detail-label">ID:</span>
                <span className="detail-value">{selectedUser.id}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Username:</span>
                <span className="detail-value">{selectedUser.username}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Email:</span>
                <span className="detail-value">{selectedUser.email}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Full Name:</span>
                <span className="detail-value">
                  {selectedUser.firstName} {selectedUser.lastName}
                </span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Phone:</span>
                <span className="detail-value">{selectedUser.phone || 'N/A'}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Status:</span>
                <span className={`status-badge ${selectedUser.active ? 'active' : 'inactive'}`}>
                  {selectedUser.active ? 'Active' : 'Inactive'}
                </span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Roles:</span>
                <div className="roles">
                  {selectedUser.roles?.map(role => (
                    <span key={role} className="role-badge">{role}</span>
                  ))}
                </div>
              </div>
              <div className="detail-row">
                <span className="detail-label">Created At:</span>
                <span className="detail-value">{formatDate(selectedUser.createdAt)}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Last Login:</span>
                <span className="detail-value">{formatDate(selectedUser.lastLogin)}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Sent Messages:</span>
                <span className="detail-value">{selectedUser.sentMessagesCount}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Received Messages:</span>
                <span className="detail-value">{selectedUser.receivedMessagesCount}</span>
              </div>
            </div>
          </div>
        </div>
      )}
      </div>
    </>
  );
};

export default UserManagement;
