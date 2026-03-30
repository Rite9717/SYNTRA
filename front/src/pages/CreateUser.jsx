import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import Navbar from '../components/Navbar';

const CreateUser = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
    role: 'ROLE_USER'
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
          await api.post('/admin/user/create', {
          username: formData.username,
          email: formData.email,
          password: formData.password,
          firstName: formData.firstName,
          lastName: formData.lastName,
          phone: formData.phone,
          role: formData.role
        });

      setSuccess(`User "${formData.username}" created successfully!`);
      setFormData({
        username: '', email: '', password: '',
        firstName: '', lastName: '', phone: '', role: 'ROLE_USER'
      });
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data || 'Failed to create user');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Navbar />
      <div style={{ maxWidth: '500px', margin: '40px auto', padding: '0 20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
          <button onClick={() => navigate('/admin/dashboard')}
            style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '20px' }}>
            ←
          </button>
          <h2 style={{ margin: 0 }}>Create New User</h2>
        </div>

        {error && (
          <div style={{ background: '#fee2e2', color: '#991b1b', padding: '12px',
            borderRadius: '8px', marginBottom: '16px' }}>
            {error}
          </div>
        )}

        {success && (
          <div style={{ background: '#dcfce7', color: '#166534', padding: '12px',
            borderRadius: '8px', marginBottom: '16px' }}>
            {success}
          </div>
        )}

        <form onSubmit={handleSubmit}
          style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>

          <div style={{ display: 'flex', gap: '12px' }}>
            <input name="firstName" placeholder="First Name" value={formData.firstName}
              onChange={handleChange}
              style={inputStyle} />
            <input name="lastName" placeholder="Last Name" value={formData.lastName}
              onChange={handleChange}
              style={inputStyle} />
          </div>

          <input name="username" placeholder="Username *" value={formData.username}
            onChange={handleChange} required style={inputStyle} />

          <input name="email" type="email" placeholder="Email *" value={formData.email}
            onChange={handleChange} required style={inputStyle} />

          <input name="password" type="password" placeholder="Password *" value={formData.password}
            onChange={handleChange} required minLength={6} style={inputStyle} />

          <input name="phone" placeholder="Phone" value={formData.phone}
            onChange={handleChange} style={inputStyle} />

          <select name="role" value={formData.role} onChange={handleChange} style={inputStyle}>
            <option value="ROLE_USER">User</option>
            <option value="ROLE_ADMIN">Admin</option>
          </select>

          <button type="submit" disabled={loading}
            style={{ padding: '12px', background: '#2563eb', color: 'white',
              border: 'none', borderRadius: '8px', cursor: 'pointer',
              fontSize: '16px', opacity: loading ? 0.7 : 1 }}>
            {loading ? 'Creating...' : 'Create User'}
          </button>
        </form>
      </div>
    </>
  );
};

const inputStyle = {
  padding: '10px 14px',
  border: '1px solid #d1d5db',
  borderRadius: '8px',
  fontSize: '15px',
  width: '100%',
  boxSizing: 'border-box'
};

export default CreateUser;