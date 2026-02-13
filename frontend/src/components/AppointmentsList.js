import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import Skeleton from 'react-loading-skeleton';
import 'react-loading-skeleton/dist/skeleton.css';
import api from '../utils/api';

function AppointmentsList({ userId, userType = 'user', limit = null, externalFilter = null }) {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState('all');

  const currentFilter = externalFilter || filter;

  const [reschedulingId, setReschedulingId] = useState(null);
  const [rescheduleData, setRescheduleData] = useState({ date: '', time: '', duration: 30 });

  const fetchAppointments = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    try {
      let url;
      if (currentFilter === 'upcoming' || limit) {
        url = userType === 'user' ? `/bookings/user/${userId}/upcoming` : `/bookings/lawyer/${userId}/upcoming`;
      } else {
        url = userType === 'user' ? `/bookings/user/${userId}` : `/bookings/lawyer/${userId}`;
      }
      const response = await api.get(url);
      let data = Array.isArray(response.data) ? response.data : [];
      if (limit) data = data.slice(0, limit);
      setAppointments(data);
    } catch (err) {
      console.error('Error fetching appointments:', err);
      setAppointments([]);
    } finally {
      setLoading(false);
    }
  }, [userId, userType, currentFilter, limit]);

  useEffect(() => {
    fetchAppointments();
  }, [fetchAppointments]);

  const getStatusStyle = (status) => {
    switch (status) {
      case 'REQUESTED': return 'bg-blue-100 text-blue-600 dark:bg-blue-500/10 dark:text-blue-400 border-blue-200 dark:border-blue-500/20';
      case 'CONFIRMED': return 'bg-emerald-100 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-400 border-emerald-200 dark:border-emerald-500/20';
      case 'RESCHEDULED': return 'bg-amber-100 text-amber-600 dark:bg-amber-500/10 dark:text-amber-400 border-amber-200 dark:border-amber-500/20';
      case 'CANCELLED': return 'bg-gray-100 text-gray-500 dark:bg-white/5 dark:text-gray-400 border-gray-200 dark:border-white/10';
      case 'COMPLETED': return 'bg-indigo-100 text-indigo-600 dark:bg-indigo-500/10 dark:text-indigo-400 border-indigo-200 dark:border-indigo-500/20';
      case 'NO_SHOW': return 'bg-rose-100 text-rose-600 dark:bg-rose-500/10 dark:text-rose-400 border-rose-200 dark:border-rose-500/20';
      default: return 'bg-slate-100 text-slate-500 border-slate-200';
    }
  };

  const handleAction = async (appointmentId, action, data = null) => {
    try {
      const headers = { [userType === 'lawyer' ? 'X-Lawyer-Id' : 'X-User-Id']: userId.toString() };
      let response;
      if (action === 'confirm') response = await api.put(`/bookings/${appointmentId}/confirm`, {}, { headers });
      else if (action === 'cancel') response = await api.put(`/bookings/${appointmentId}/cancel`, {}, { headers });
      else if (action === 'complete') response = await api.put(`/bookings/${appointmentId}/complete`, {}, { headers });
      else if (action === 'reschedule') {
        const payload = {
          appointmentDate: `${data.date}T${data.time}`,
          durationMinutes: parseInt(data.duration),
          lawyerId: data.lawyerId
        };
        response = await api.put(`/bookings/${appointmentId}/reschedule`, payload, { headers });
      }

      if (response.data.success) {
        toast.success(response.data.message);
        setReschedulingId(null);
        fetchAppointments();
      }
    } catch (err) {
      toast.error(err.response?.data?.message || `Operation failed`);
    }
  };

  const renderActions = (apt) => {
    const isCounterParty = apt.requestedByRole !== userType;
    const canConfirm = (apt.status === 'REQUESTED' || apt.status === 'RESCHEDULED') && isCounterParty;
    const canJoin = apt.status === 'CONFIRMED';
    const canComplete = apt.status === 'CONFIRMED' && userType === 'lawyer';
    const canCancel = ['REQUESTED', 'CONFIRMED', 'RESCHEDULED'].includes(apt.status);

    if (reschedulingId === apt.id) {
      return (
        <div className="mt-6 p-4 bg-slate-50 dark:bg-white/5 rounded-2xl border border-slate-200 dark:border-slate-800 space-y-4">
          <p className="text-[10px] font-black text-primary uppercase tracking-widest text-center">Propose New Temporal Slot</p>
          <div className="grid grid-cols-2 gap-3">
            <input
              type="date"
              className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-2 rounded-lg text-xs font-bold dark:text-white outline-none focus:ring-1 focus:ring-primary"
              value={rescheduleData.date}
              min={new Date().toISOString().split('T')[0]}
              onChange={(e) => setRescheduleData({ ...rescheduleData, date: e.target.value })}
            />
            <input
              type="time"
              className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-2 rounded-lg text-xs font-bold dark:text-white outline-none focus:ring-1 focus:ring-primary"
              value={rescheduleData.time}
              onChange={(e) => setRescheduleData({ ...rescheduleData, time: e.target.value })}
            />
          </div>
          <select
            className="w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-2 rounded-lg text-xs font-bold dark:text-white outline-none focus:ring-1 focus:ring-primary"
            value={rescheduleData.duration}
            onChange={(e) => setRescheduleData({ ...rescheduleData, duration: e.target.value })}
          >
            <option value="15">15 Minutes</option>
            <option value="30">30 Minutes</option>
            <option value="60">60 Minutes</option>
          </select>
          <div className="flex gap-2">
            <button
              onClick={() => handleAction(apt.id, 'reschedule', { ...rescheduleData, lawyerId: apt.lawyerId })}
              disabled={!rescheduleData.date || !rescheduleData.time}
              className="flex-1 py-2.5 bg-primary text-white rounded-lg font-black uppercase tracking-widest text-[9px] hover:opacity-90 disabled:opacity-50 transition-all"
            >
              Propose
            </button>
            <button
              onClick={() => setReschedulingId(null)}
              className="flex-1 py-2.5 bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400 rounded-lg font-black uppercase tracking-widest text-[9px] hover:bg-slate-300 dark:hover:bg-slate-700 transition-all"
            >
              Cancel
            </button>
          </div>
        </div>
      );
    }

    // Check if this appointment is in a terminal state (no actions available)
    const isTerminal = ['CANCELLED', 'COMPLETED', 'NO_SHOW'].includes(apt.status);
    const hasNoActions = !canConfirm && !canJoin && !canComplete && !canCancel;

    if (hasNoActions && isTerminal) {
      return (
        <div className="mt-6 py-3 px-4 bg-slate-50 dark:bg-slate-800/50 rounded-xl border border-slate-200 dark:border-slate-700 text-center">
          <p className="text-[9px] font-black text-slate-400 uppercase tracking-[0.2em]">Session Terminated</p>
        </div>
      );
    }

    return (
      <div className="flex flex-wrap gap-2 mt-6">
        {canConfirm && (
          <button onClick={() => handleAction(apt.id, 'confirm')} className="flex-1 py-3 bg-emerald-500 text-white rounded-xl font-black uppercase tracking-widest text-[9px] hover:bg-emerald-600 transition-all flex items-center justify-center gap-2 shadow-lg shadow-emerald-500/20">
            <span className="material-symbols-outlined !text-sm">check_circle</span>
            Authorize
          </button>
        )}
        {apt.status === 'REQUESTED' && isCounterParty && (
          <button
            onClick={() => {
              setReschedulingId(apt.id);
              const dt = new Date(apt.appointmentDate);
              setRescheduleData({
                date: dt.toISOString().split('T')[0],
                time: dt.toTimeString().split(' ')[0].substring(0, 5),
                duration: apt.durationMinutes
              });
            }}
            className="flex-1 py-3 bg-amber-500 text-white rounded-xl font-black uppercase tracking-widest text-[9px] hover:bg-amber-600 transition-all flex items-center justify-center gap-2 shadow-lg shadow-amber-500/20"
          >
            <span className="material-symbols-outlined !text-sm">event_repeat</span>
            Reschedule
          </button>
        )}
        {canJoin && (
          <button onClick={() => window.open('https://meet.google.com/new', '_blank')} className="flex-1 py-3 bg-primary text-white rounded-xl font-black uppercase tracking-widest text-[9px] hover:opacity-90 transition-all flex items-center justify-center gap-2 shadow-lg shadow-primary/20">
            <span className="material-symbols-outlined !text-sm">video_chat</span>
            Join Link
          </button>
        )}
        {canComplete && (
          <button onClick={() => handleAction(apt.id, 'complete')} className="flex-1 py-3 bg-indigo-500 text-white rounded-xl font-black uppercase tracking-widest text-[9px] hover:bg-indigo-600 transition-all flex items-center justify-center gap-2 shadow-lg shadow-indigo-500/20">
            <span className="material-symbols-outlined !text-sm">task_alt</span>
            Complete
          </button>
        )}
        {canCancel && (
          <button onClick={() => handleAction(apt.id, 'cancel')} className="px-4 py-3 bg-rose-500/10 text-rose-500 rounded-xl font-black uppercase tracking-widest text-[9px] hover:bg-rose-500 hover:text-white transition-all flex items-center gap-2 border border-rose-500/20">
            <span className="material-symbols-outlined !text-sm">cancel</span>
            Cancel
          </button>
        )}
      </div>
    );
  };

  if (loading) return <div className="space-y-4">{[1, 2, 3].map(i => <Skeleton key={i} height={80} borderRadius={16} />)}</div>;

  if (appointments.length === 0) {
    return (
      <div className="text-center py-12 bg-slate-50 dark:bg-white/5 rounded-[2rem] border border-dashed border-slate-200 dark:border-slate-800">
        <span className="material-symbols-outlined text-4xl text-slate-300 dark:text-slate-700 mb-4">event_busy</span>
        <p className="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em]">Zero Authorized Sessions Found</p>
      </div>
    );
  }

  // Compact sidebar view for dashboard widgets
  if (limit) {
    return (
      <div className="space-y-4">
        {appointments.map((apt) => {
          const canJoin = apt.status === 'CONFIRMED';
          const canCancel = ['REQUESTED', 'CONFIRMED', 'RESCHEDULED'].includes(apt.status);

          return (
            <div key={apt.id} className="bg-slate-50 dark:bg-white/5 rounded-2xl p-5 border border-slate-100 dark:border-slate-800 space-y-4">
              <div className="flex items-center gap-3 mb-4">
                <div className="size-12 rounded-xl bg-white dark:bg-slate-800 flex items-center justify-center border border-slate-200 dark:border-slate-700">
                  <span className="material-symbols-outlined text-slate-400">account_circle</span>
                </div>
                <div className="flex-1">
                  <h4 className="font-bold text-sm text-slate-900 dark:text-white">
                    {userType === 'user' ? (apt.lawyerFullName || 'Attorney') : (apt.userFullName || 'Client')}
                  </h4>
                  <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Counter-Party</p>
                </div>
              </div>

              <div className="space-y-3">
                <div className="flex items-center gap-2 text-slate-600 dark:text-slate-400">
                  <span className="material-symbols-outlined !text-base">calendar_today</span>
                  <span className="text-xs font-bold">
                    {new Date(apt.appointmentDate).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-slate-600 dark:text-slate-400">
                    <span className="material-symbols-outlined !text-base">schedule</span>
                    <span className="text-xs font-bold">
                      {new Date(apt.appointmentDate).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                  <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest">
                    {apt.durationMinutes}m Span
                  </span>
                </div>
                <div className="flex items-center gap-2 text-slate-600 dark:text-slate-400">
                  <span className="material-symbols-outlined !text-base">
                    {apt.meetingType === 'video' ? 'videocam' : 'call'}
                  </span>
                  <span className="text-[10px] font-black uppercase tracking-widest">
                    {apt.meetingType} Transceiver
                  </span>
                </div>
              </div>

              <div className="flex flex-col gap-2 pt-2">
                {canJoin && (
                  <button
                    onClick={() => window.open('https://meet.google.com/new', '_blank')}
                    className="w-full py-3 bg-primary text-white rounded-xl font-black uppercase tracking-widest text-[9px] hover:opacity-90 transition-all flex items-center justify-center gap-2 shadow-lg shadow-primary/20"
                  >
                    <span className="material-symbols-outlined !text-sm">video_chat</span>
                    Join Link
                  </button>
                )}
                {canCancel && (
                  <button
                    onClick={() => handleAction(apt.id, 'cancel')}
                    className="w-full py-2.5 bg-rose-500/10 text-rose-500 rounded-xl font-black uppercase tracking-widest text-[9px] hover:bg-rose-500 hover:text-white transition-all flex items-center justify-center gap-2 border border-rose-500/20"
                  >
                    <span className="material-symbols-outlined !text-sm">cancel</span>
                    Cancel
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    );
  }

  // Full grid view for appointments page
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 animate-in fade-in slide-in-from-bottom-4 duration-700">
      {appointments.map((apt) => {
        const isTerminal = ['CANCELLED', 'COMPLETED', 'NO_SHOW'].includes(apt.status);

        return (
          <div key={apt.id} className={`group bg-white dark:bg-gray-900 rounded-[2.5rem] border border-slate-200 dark:border-slate-800 p-6 shadow-sm hover:shadow-xl transition-all duration-500 relative overflow-hidden ${isTerminal ? 'opacity-60' : ''}`}>
            <div className="flex justify-between items-start mb-6">
              <div className="flex items-center gap-4">
                <div className="size-12 rounded-2xl bg-slate-50 dark:bg-white/5 flex items-center justify-center border border-slate-100 dark:border-white/10">
                  <span className="material-symbols-outlined text-slate-400">account_circle</span>
                </div>
                <div>
                  <h4 className="text-sm font-black text-slate-900 dark:text-white truncate max-w-[120px]">
                    {userType === 'user' ? apt.lawyerFullName : apt.userFullName}
                  </h4>
                  <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mt-0.5">Counter-Party</p>
                </div>
              </div>
              <span className={`px-3 py-1 rounded-full text-[9px] font-black uppercase border ${getStatusStyle(apt.status)}`}>
                {apt.status}
              </span>
            </div>

            <div className="space-y-4 mb-2">
              <div className="flex items-center gap-3 text-slate-600 dark:text-slate-400">
                <span className="material-symbols-outlined !text-lg">calendar_today</span>
                <p className="text-xs font-bold">{new Date(apt.appointmentDate).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })}</p>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3 text-slate-600 dark:text-slate-400">
                  <span className="material-symbols-outlined !text-lg">schedule</span>
                  <p className="text-xs font-bold">{new Date(apt.appointmentDate).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                </div>
                <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest">{apt.durationMinutes}m Span</span>
              </div>
              <div className="flex items-center gap-3 text-slate-600 dark:text-slate-400">
                <span className="material-symbols-outlined !text-lg">{apt.meetingType === 'video' ? 'videocam' : 'call'}</span>
                <p className="text-[10px] font-black uppercase tracking-widest">{apt.meetingType} Transceiver</p>
              </div>
            </div>

            {renderActions(apt)}
          </div>
        );
      })}
    </div>
  );
}

export default AppointmentsList;
