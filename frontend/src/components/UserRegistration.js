import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';


const API_BASE_URL = 'http://localhost:8080/api/auth';

function UserRegistration() {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    fullName: '',
    email: ''
  });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    // Clear error for this field when user starts typing
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const validateForm = () => {
    const newErrors = {};

    // Username validation
    if (!formData.username.trim()) {
      newErrors.username = 'Username is required';
    } else if (formData.username.length < 3) {
      newErrors.username = 'Username must be at least 3 characters';
    } else if (formData.username.length > 100) {
      newErrors.username = 'Username must not exceed 100 characters';
    }

    // Password validation
    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else {
      const password = formData.password;
      if (password.length < 8) {
        newErrors.password = 'Password must be at least 8 characters long';
      } else if (!/[A-Z]/.test(password)) {
        newErrors.password = 'Include at least one uppercase letter';
      } else if (!/[a-z]/.test(password)) {
        newErrors.password = 'Include at least one lowercase letter';
      } else if (!/[0-9]/.test(password)) {
        newErrors.password = 'Include at least one digit';
      } else if (!/[^A-Za-z0-9]/.test(password)) {
        newErrors.password = 'Include at least one special character';
      }
    }

    // Confirm password validation
    if (!formData.confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your password';
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    // Full name validation
    if (!formData.fullName.trim()) {
      newErrors.fullName = 'Full name is required';
    } else if (formData.fullName.length > 255) {
      newErrors.fullName = 'Full name must not exceed 255 characters';
    }

    // Email validation
    if (formData.email && formData.email.trim()) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(formData.email)) {
        newErrors.email = 'Please enter a valid email address';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!validateForm()) {
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/user/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: formData.username.trim(),
          password: formData.password,
          fullName: formData.fullName.trim(),
          email: formData.email.trim() || null
        }),
      });

      let data;
      try {
        data = await response.json();
      } catch (jsonError) {
        console.error('Failed to parse response as JSON:', jsonError);
        setError('Server returned an invalid response. Please try again.');
        setLoading(false);
        return;
      }

      if (response.ok && data.success) {
        // Registration successful - redirect to login
        toast.success('Registration successful! Please login with your credentials.');
        navigate('/user-login');
      } else {
        const errorMsg = data.message || 'Registration failed. Please try again.';
        setError(errorMsg);
        toast.error(errorMsg);
      }
    } catch (err) {
      console.error('Registration error:', err);
      setError('Error connecting to server. Please make sure the backend is running on http://localhost:8080');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background-light dark:bg-background-dark flex items-center justify-center p-6 geo-pattern hero-gradient">
      <div className="glass-card w-full max-w-lg rounded-[2.5rem] p-10 border border-white/50 shadow-2xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-40 h-40 bg-electric-blue/10 rounded-full blur-3xl -mr-20 -mt-20"></div>

        <div className="flex flex-col items-center mb-8 text-center">
          <div className="w-16 h-16 bg-primary rounded-2xl flex items-center justify-center text-white mb-4 shadow-lg shadow-primary/20">
            <span className="material-symbols-outlined text-3xl">app_registration</span>
          </div>
          <h1 className="text-3xl font-black text-primary dark:text-white tracking-tight uppercase">Register</h1>
          <p className="text-sm text-gray-500 font-bold mt-1 uppercase tracking-widest leading-relaxed">Create Your Secure Identity</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div className="space-y-2">
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Identifier *</label>
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                placeholder="Username"
                className={`w-full px-6 py-4 bg-white/50 border-2 rounded-2xl focus:bg-white transition-all outline-none font-bold text-primary ${errors.username ? 'border-red-500/50' : 'border-transparent focus:border-electric-blue'}`}
              />
              {errors.username && <p className="text-[10px] text-red-500 font-black uppercase px-4">{errors.username}</p>}
            </div>

            <div className="space-y-2">
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Full Identity *</label>
              <input
                type="text"
                name="fullName"
                value={formData.fullName}
                onChange={handleChange}
                placeholder="Full Name"
                className={`w-full px-6 py-4 bg-white/50 border-2 rounded-2xl focus:bg-white transition-all outline-none font-bold text-primary ${errors.fullName ? 'border-red-500/50' : 'border-transparent focus:border-electric-blue'}`}
              />
              {errors.fullName && <p className="text-[10px] text-red-500 font-black uppercase px-4">{errors.fullName}</p>}
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Digital Link (Optional)</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="Email Address"
              className={`w-full px-6 py-4 bg-white/50 border-2 rounded-2xl focus:bg-white transition-all outline-none font-bold text-primary ${errors.email ? 'border-red-500/50' : 'border-transparent focus:border-electric-blue'}`}
            />
            {errors.email && <p className="text-[10px] text-red-500 font-black uppercase px-4">{errors.email}</p>}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div className="space-y-2">
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Secret *</label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="Password"
                className={`w-full px-6 py-4 bg-white/50 border-2 rounded-2xl focus:bg-white transition-all outline-none font-bold text-primary ${errors.password ? 'border-red-500/50' : 'border-transparent focus:border-electric-blue'}`}
              />
              <div className="px-4 pt-1 space-y-1">
                <p className="text-[9px] font-bold text-gray-400 uppercase tracking-wider">
                  Min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special
                </p>
                {errors.password && <p className="text-[10px] text-red-500 font-black uppercase">{errors.password}</p>}
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Confirm *</label>
              <input
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                placeholder="Confirm"
                className={`w-full px-6 py-4 bg-white/50 border-2 rounded-2xl focus:bg-white transition-all outline-none font-bold text-primary ${errors.confirmPassword ? 'border-red-500/50' : 'border-transparent focus:border-electric-blue'}`}
              />
              {errors.confirmPassword && <p className="text-[10px] text-red-500 font-black uppercase px-4">{errors.confirmPassword}</p>}
            </div>
          </div>

          {error && (
            <div className="bg-red-500/10 border border-red-500/20 text-red-500 text-xs font-bold p-4 rounded-xl flex items-center gap-2">
              <span className="material-symbols-outlined text-sm">report_problem</span>
              {error}
            </div>
          )}

          <button
            type="submit"
            className="w-full h-16 bg-electric-blue text-white rounded-2xl font-black text-lg shadow-xl shadow-electric-blue/30 hover:scale-[1.02] transition-all flex items-center justify-center gap-2 disabled:opacity-50"
            disabled={loading}
          >
            {loading ? (
              <span className="animate-spin material-symbols-outlined">refresh</span>
            ) : (
              <>
                Create Account
                <span className="material-symbols-outlined">person_add</span>
              </>
            )}
          </button>
        </form>

        <div className="mt-8 pt-8 border-t border-gray-100 text-center">
          <p className="text-sm font-bold text-gray-500">
            Already have an active identity? <Link to="/user-login" className="text-electric-blue hover:underline">Sign In Here</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default UserRegistration;

