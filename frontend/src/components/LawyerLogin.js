import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';
import api from '../utils/api';
import './Login.css';

function LawyerLogin() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    if (!username || !password) {
      setError('Please enter both username and password');
      setLoading(false);
      return;
    }

    try {
      const response = await api.post('/auth/lawyer/login', { username, password });
      const data = response.data;

      if (data.success) {
        const lawyerData = {
          username: data.username || username,
          fullName: data.fullName || '',
          id: data.id || ''
        };
        const token = data.token || '';

        login('lawyer', token, lawyerData);

        toast.success('Login successful!');
        navigate('/lawyer-dashboard');
      } else {
        const errorMsg = data.message || 'Invalid username or password';
        setError(errorMsg);
        toast.error(errorMsg);
      }
    } catch (err) {
      console.error('Login error:', err);
      const errorMsg = err.response?.data?.message || 'Error connecting to server. Please try again later.';
      setError(errorMsg);
      toast.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h1 className="login-title">Lawyer Login</h1>
        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter your username"
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              required
            />
          </div>
          {error && <div className="error-message">{error}</div>}
          <button type="submit" className="login-button" disabled={loading}>
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>
        <div className="login-footer">
          <p>Are you a user? <a href="/user-login">Login here</a></p>
        </div>
      </div>
    </div>
  );
}

export default LawyerLogin;

