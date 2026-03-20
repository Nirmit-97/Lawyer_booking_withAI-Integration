import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { authApi } from '../utils/api';

function ForgotPassword() {
  const [step, setStep] = useState(1); // 1 = Email, 2 = OTP + New Password
  const [email, setEmail] = useState('');
  const [userType, setUserType] = useState('user');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleRequestOtp = async (e) => {
    e.preventDefault();
    setError('');
    
    if (!email) {
      setError('Please enter your email.');
      return;
    }

    setLoading(true);
    try {
      await authApi.forgotPassword({ email, userType });
      toast.success('OTP sent to your email! Please check your inbox.');
      setStep(2);
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.response?.data?.error || 'Failed to send OTP. Please check your email and try again.';
      setError(errorMsg);
      toast.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setError('');

    if (!otp || !newPassword || !confirmPassword) {
      setError('Please fill in all fields.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    if (newPassword.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    setLoading(true);
    try {
      await authApi.resetPassword({ email, userType, otp, newPassword });
      toast.success('Password reset successfully! You can now log in.');
      navigate(userType === 'lawyer' ? '/lawyer-login' : '/user-login');
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.response?.data?.error || 'Invalid OTP or failed to reset password.';
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
             <span className="material-symbols-outlined text-3xl">lock_reset</span>
          </div>
          <h1 className="text-3xl font-black text-primary dark:text-white tracking-tight uppercase">Reset Password</h1>
          <p className="text-sm text-gray-500 font-bold mt-2 uppercase tracking-widest">
            {step === 1 ? 'Recover Your Access' : 'Create New Password'}
          </p>
        </div>

        {error && (
          <div className="bg-red-500/10 border border-red-500/20 text-red-500 text-xs font-bold p-4 rounded-xl flex items-center gap-2 mb-6">
            <span className="material-symbols-outlined text-sm">error</span>
            {error}
          </div>
        )}

        {step === 1 ? (
          <form onSubmit={handleRequestOtp} className="space-y-6">
            
            <div className="space-y-2">
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Account Type</label>
              <div className="flex gap-4 p-2 bg-white/50 rounded-2xl">
                 <button type="button" onClick={() => setUserType('user')} className={`flex-1 py-3 px-4 rounded-xl font-bold transition-all text-sm ${userType === 'user' ? 'bg-primary text-white shadow-md' : 'text-gray-500 hover:bg-white/50'}`}>User</button>
                 <button type="button" onClick={() => setUserType('lawyer')} className={`flex-1 py-3 px-4 rounded-xl font-bold transition-all text-sm ${userType === 'lawyer' ? 'bg-primary text-white shadow-md' : 'text-gray-500 hover:bg-white/50'}`}>Lawyer</button>
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Registered Email</label>
              <div className="relative group">
                <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-electric-blue transition-colors">email</span>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Enter your email"
                  className="w-full pl-12 pr-6 py-4 bg-white/50 border-2 border-transparent rounded-2xl focus:border-electric-blue focus:bg-white transition-all outline-none font-bold text-primary"
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              className="w-full h-16 bg-electric-blue text-white rounded-2xl font-black text-lg shadow-xl shadow-electric-blue/30 hover:scale-[1.02] hover:shadow-electric-blue/40 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
              disabled={loading}
            >
              {loading ? <span className="animate-spin material-symbols-outlined">sync</span> : 'Send Recovery OTP'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleResetPassword} className="space-y-6">
            
            <div className="space-y-2">
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Verification OTP</label>
              <div className="relative group">
                <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-electric-blue transition-colors">pin</span>
                <input
                  type="text"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  placeholder="Enter 6-digit OTP"
                  className="w-full pl-12 pr-6 py-4 bg-white/50 border-2 border-transparent rounded-2xl focus:border-electric-blue focus:bg-white transition-all outline-none font-bold text-primary"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">New Password</label>
              <div className="relative group">
                <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-electric-blue transition-colors">lock</span>
                <input
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Enter new password"
                  className="w-full pl-12 pr-6 py-4 bg-white/50 border-2 border-transparent rounded-2xl focus:border-electric-blue focus:bg-white transition-all outline-none font-bold text-primary"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Confirm Password</label>
              <div className="relative group">
                <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-electric-blue transition-colors">lock</span>
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Confirm new password"
                  className="w-full pl-12 pr-6 py-4 bg-white/50 border-2 border-transparent rounded-2xl focus:border-electric-blue focus:bg-white transition-all outline-none font-bold text-primary"
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              className="w-full h-16 bg-emerald-500 text-white rounded-2xl font-black text-lg shadow-xl shadow-emerald-500/30 hover:scale-[1.02] hover:shadow-emerald-500/40 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
              disabled={loading}
            >
              {loading ? <span className="animate-spin material-symbols-outlined">sync</span> : 'Update Password'}
            </button>
            <button type="button" onClick={() => setStep(1)} className="w-full py-4 text-sm font-bold text-gray-500 hover:text-primary transition-colors">
              Back to Email
            </button>
          </form>
        )}

        <div className="mt-8 pt-8 border-t border-gray-100 flex flex-col items-center gap-4 text-center">
          <p className="text-sm font-bold text-gray-500">
             Remember your details? <Link to="/user-login" className="text-electric-blue hover:underline">Back to Login</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default ForgotPassword;
