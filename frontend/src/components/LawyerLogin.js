import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';
import api from '../utils/api';


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
        const refreshToken = data.refreshToken || '';

        login('lawyer', token, lawyerData, refreshToken);

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
    <div className="min-h-screen bg-background-light dark:bg-background-dark flex items-center justify-center p-6 geo-pattern hero-gradient">
      <div className="glass-card w-full max-w-md rounded-[2.5rem] p-10 border border-white/50 shadow-2xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-32 h-32 bg-electric-blue/10 rounded-full blur-3xl -mr-16 -mt-16"></div>

        <div className="flex flex-col items-center mb-10 text-center">
          <div className="w-16 h-16 bg-primary rounded-2xl flex items-center justify-center text-white mb-4 shadow-lg shadow-primary/20">
            <span className="material-symbols-outlined text-3xl">gavel</span>
          </div>
          <h1 className="text-3xl font-black text-primary dark:text-white tracking-tight uppercase">Lawyer Login</h1>
          <p className="text-sm text-gray-500 font-bold mt-2 uppercase tracking-widest">Authority Access Portal</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-2">
            <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4" htmlFor="username">Expert Identifier</label>
            <div className="relative group">
              <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-electric-blue transition-colors">person</span>
              <input
                type="text"
                id="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter expert ID"
                className="w-full pl-12 pr-6 py-4 bg-white/50 border-2 border-transparent rounded-2xl focus:border-electric-blue focus:bg-white transition-all outline-none font-bold text-primary"
                required
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4" htmlFor="password">Security Credential</label>
            <div className="relative group">
              <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-electric-blue transition-colors">lock</span>
              <input
                type="password"
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter password"
                className="w-full pl-12 pr-6 py-4 bg-white/50 border-2 border-transparent rounded-2xl focus:border-electric-blue focus:bg-white transition-all outline-none font-bold text-primary"
                required
              />
            </div>
          </div>

          {error && (
            <div className="bg-red-500/10 border border-red-500/20 text-red-500 text-xs font-bold p-4 rounded-xl flex items-center gap-2">
              <span className="material-symbols-outlined text-sm">error</span>
              {error}
            </div>
          )}

          <button
            type="submit"
            className="w-full h-16 bg-electric-blue text-white rounded-2xl font-black text-lg shadow-xl shadow-electric-blue/30 hover:scale-[1.02] hover:shadow-electric-blue/40 transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:scale-100"
            disabled={loading}
          >
            {loading ? (
              <span className="animate-spin material-symbols-outlined">sync</span>
            ) : (
              <>
                Verify Authority
                <span className="material-symbols-outlined">arrow_forward</span>
              </>
            )}
          </button>
        </form>

        <div className="mt-8 pt-8 border-t border-gray-100 flex flex-col items-center gap-4 text-center">
          <p className="text-sm font-bold text-gray-500">
            Standard user? <a href="/user-login" className="text-electric-blue hover:underline">Access Client Portal</a>
          </p>
          <p className="text-sm font-bold text-gray-500">
            Join the network? <a href="/lawyer-register" className="text-electric-blue hover:underline">Register as Counsel</a>
          </p>
          <p className="text-[10px] font-black uppercase tracking-widest text-gray-300 mt-2">
            System Administrator? <a href="/admin/login" className="hover:text-primary transition-colors">Maintenance Access</a>
          </p>
        </div>
      </div>
    </div>
  );
}

export default LawyerLogin;

