import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import Skeleton from 'react-loading-skeleton';
import 'react-loading-skeleton/dist/skeleton.css';
import api from '../utils/api'; // Assuming 'api' is the general API utility

// If bookingApi is a separate utility, you might need to import it like this:
// import bookingApi from '../utils/bookingApi'; // Or define it if it's a wrapper around 'api'

// For the purpose of this edit, I will assume `bookingApi` refers to the `api` utility
// and adjust the calls accordingly, or if it's a new utility, I'll add a placeholder.
// Given the instruction uses `bookingApi.getLawyers()` and `bookingApi.create()`,
// I will assume `bookingApi` is a separate utility and add a placeholder for it.
// If `api` is meant to be `bookingApi`, then the calls would be `api.get('/bookings/lawyers')` etc.
// Let's define a simple `bookingApi` wrapper for `api` for consistency with the provided snippet.
const bookingApi = {
  getLawyers: () => api.get('/bookings/lawyers'),
  create: (userId, data) => api.post('/bookings/create', data, { headers: { 'X-User-Id': userId.toString() } })
};


function Booking({ userId, onBookingSuccess, userType, preSelectedLawyerId, preSelectedCaseId, preSelectedLawyerName }) {
  const [lawyers, setLawyers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fetchingLawyers, setFetchingLawyers] = useState(false); // New state for fetching lawyers
  const [error, setError] = useState('');
  const [formData, setFormData] = useState({
    lawyerId: preSelectedLawyerId || '', // Initialize with preSelectedLawyerId
    appointmentDate: '',
    appointmentTime: '',
    durationMinutes: 60,
    meetingType: 'video',
    description: '',
    caseId: preSelectedCaseId || '', // New field
    notes: '', // New field
    requestedByRole: userType || 'user' // New field
  });

  useEffect(() => {
    // Only fetch lawyers if no pre-selected lawyer is provided
    if (!preSelectedLawyerId) {
      fetchLawyers();
    }
  }, [preSelectedLawyerId]); // Dependency on preSelectedLawyerId

  const fetchLawyers = async () => {
    setFetchingLawyers(true); // Use new state for fetching lawyers
    setError('');
    try {
      const response = await bookingApi.getLawyers(); // Use bookingApi
      setLawyers(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error('Error fetching lawyers:', err);
      const errorMsg = 'Failed to load lawyers'; // Updated error message
      setError(errorMsg);
      toast.error(errorMsg);
      setLawyers([]);
    } finally {
      setFetchingLawyers(false); // Use new state for fetching lawyers
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const dateTimeString = `${formData.appointmentDate}T${formData.appointmentTime}`;
      const submitData = {
        ...formData,
        appointmentDate: dateTimeString,
        lawyerId: parseInt(formData.lawyerId),
      };

      await bookingApi.create(userId, submitData);
      toast.success('Consultation Logged: Awaiting Authorization');
      if (onBookingSuccess) onBookingSuccess();
    } catch (err) {
      console.error('Error booking appointment:', err);
      const msg = err.response?.data?.message || 'Failed to book appointment';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  // Set minimum date to today
  const today = new Date().toISOString().split('T')[0];

  return (
    <div className="max-w-4xl mx-auto animate-in fade-in slide-in-from-bottom-4 duration-700">
      <div className="bg-white dark:bg-gray-900 rounded-[2.5rem] border border-slate-200 dark:border-slate-800 p-8 shadow-sm">
        <div className="flex items-center gap-4 mb-10">
          <div className="size-14 bg-primary/10 rounded-2xl flex items-center justify-center text-primary">
            <span className="material-symbols-outlined text-3xl">event_available</span>
          </div>
          <div>
            <h2 className="text-2xl font-black text-slate-900 dark:text-white uppercase tracking-tight">System Consultation</h2>
            <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mt-1">Establishing Secure Temporal Link</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            {/* Lawyer Selection */}
            <div className="space-y-3">
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400">Target Counsel</label>
              {preSelectedLawyerId ? (
                <div className="w-full bg-slate-50 dark:bg-white/5 border border-slate-200 dark:border-slate-800 p-4 rounded-xl text-slate-900 dark:text-white font-bold flex items-center gap-3">
                  <span className="material-symbols-outlined text-primary">verified_user</span>
                  {preSelectedLawyerName || 'Assigned Expert'}
                </div>
              ) : (
                <select
                  name="lawyerId"
                  value={formData.lawyerId}
                  onChange={handleChange}
                  required
                  className="w-full bg-slate-50 dark:bg-white/5 border border-slate-200 dark:border-slate-800 p-4 rounded-xl focus:ring-2 focus:ring-primary/20 outline-none transition-all dark:text-white font-bold"
                >
                  <option value="">Select Primary Counsel</option>
                  {lawyers.map(l => (
                    <option key={l.id} value={l.id}>{l.fullName}</option>
                  ))}
                </select>
              )}
            </div>

            {/* Mode Selection */}
            <div className="space-y-3">
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400">Transmission Mode</label>
              <div className="flex gap-4">
                {['video', 'phone'].map(mode => (
                  <button
                    key={mode}
                    type="button"
                    onClick={() => setFormData({ ...formData, meetingType: mode })}
                    className={`flex-1 p-4 rounded-xl border flex items-center justify-center gap-2 transition-all font-bold text-[10px] uppercase tracking-widest ${formData.meetingType === mode ? 'bg-primary border-primary text-white shadow-lg' : 'bg-slate-50 dark:bg-white/5 border-slate-200 dark:border-slate-800 text-slate-500 hover:border-primary/50'}`}
                  >
                    <span className="material-symbols-outlined !text-lg">
                      {mode === 'video' ? 'videocam' : 'call'}
                    </span>
                    {mode}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div>
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 block mb-3">Temporal Date</label>
              <input
                type="date"
                name="appointmentDate"
                value={formData.appointmentDate}
                onChange={handleChange}
                min={today}
                required
                className="w-full bg-slate-50 dark:bg-white/5 border border-slate-200 dark:border-slate-800 p-4 rounded-xl focus:ring-2 focus:ring-primary/20 outline-none transition-all dark:text-white font-bold"
              />
            </div>
            <div>
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 block mb-3">Target Time</label>
              <input
                type="time"
                name="appointmentTime"
                value={formData.appointmentTime}
                onChange={handleChange}
                required
                className="w-full bg-slate-50 dark:bg-white/5 border border-slate-200 dark:border-slate-800 p-4 rounded-xl focus:ring-2 focus:ring-primary/20 outline-none transition-all dark:text-white font-bold"
              />
            </div>
            <div>
              <label className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 block mb-3">Operational Span</label>
              <select
                name="durationMinutes"
                value={formData.durationMinutes}
                onChange={handleChange}
                className="w-full bg-slate-50 dark:bg-white/5 border border-slate-200 dark:border-slate-800 p-4 rounded-xl focus:ring-2 focus:ring-primary/20 outline-none transition-all dark:text-white font-bold"
              >
                <option value="15">15 Minutes (Brief)</option>
                <option value="30">30 Minutes (Standard)</option>
                <option value="60">60 Minutes (Strategic)</option>
              </select>
            </div>
          </div>

          <div className="space-y-3">
            <label className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400">Matter Context</label>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleChange}
              rows="3"
              className="w-full bg-slate-50 dark:bg-white/5 border border-slate-200 dark:border-slate-800 p-4 rounded-xl focus:ring-2 focus:ring-primary/20 outline-none transition-all dark:text-white font-medium text-sm resize-none"
              placeholder="Provide strategic brief for the consultation..."
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-5 bg-primary text-white rounded-2xl font-black uppercase tracking-[0.2em] text-[10px] shadow-xl shadow-primary/20 hover:opacity-95 transition-all flex items-center justify-center gap-3 disabled:opacity-50"
          >
            {loading ? (
              <span className="animate-spin material-symbols-outlined text-lg">sync</span>
            ) : (
              <>
                <span className="material-symbols-outlined text-lg">verified_user</span>
                Initiate Authorization Request
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}

export default Booking;

