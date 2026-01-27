import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import Skeleton from 'react-loading-skeleton';
import 'react-loading-skeleton/dist/skeleton.css';
import api from '../utils/api';
import './Booking.css';

function AppointmentsList({ userId, userType = 'user' }) {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('all'); // all, upcoming, past

  const fetchAppointments = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      let url;
      if (filter === 'upcoming') {
        url = userType === 'user'
          ? `/bookings/user/${userId}/upcoming`
          : `/bookings/lawyer/${userId}/upcoming`;
      } else {
        url = userType === 'user'
          ? `/bookings/user/${userId}`
          : `/bookings/lawyer/${userId}`;
      }

      const response = await api.get(url);
      setAppointments(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error('Error fetching appointments:', err);
      const errorMsg = 'Error loading appointments';
      setError(errorMsg);
      toast.error(errorMsg);
      setAppointments([]);
    } finally {
      setLoading(false);
    }
  }, [userId, userType, filter]);

  useEffect(() => {
    fetchAppointments();
  }, [fetchAppointments]);

  const handleCancel = async (appointmentId) => {
    if (!window.confirm('Are you sure you want to cancel this appointment?')) {
      return;
    }

    try {
      const response = await api.put(`/bookings/${appointmentId}/cancel`, {}, {
        headers: {
          'X-User-Id': userId.toString()
        }
      });

      if (response.data.success) {
        toast.success('Appointment cancelled successfully');
        fetchAppointments(); // Refresh list
      } else {
        const errorMsg = response.data.message || 'Failed to cancel appointment';
        toast.error(errorMsg);
      }
    } catch (err) {
      console.error('Error cancelling appointment:', err);
      toast.error('Error cancelling appointment. Please try again.');
    }
  };

  const handleConfirm = async (appointmentId) => {
    try {
      const response = await api.put(`/bookings/${appointmentId}/confirm`, {}, {
        headers: {
          'X-Lawyer-Id': userId.toString()
        }
      });

      if (response.data.success) {
        toast.success('Appointment confirmed successfully');
        fetchAppointments(); // Refresh list
      } else {
        const errorMsg = response.data.message || 'Failed to confirm appointment';
        toast.error(errorMsg);
      }
    } catch (err) {
      console.error('Error confirming appointment:', err);
      toast.error('Error confirming appointment. Please try again.');
    }
  };

  const formatDateTime = (dateTimeString) => {
    const date = new Date(dateTimeString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusBadge = (status) => {
    const statusColors = {
      pending: '#f39c12',
      confirmed: '#27ae60',
      completed: '#3498db',
      cancelled: '#e74c3c'
    };

    return (
      <span
        className="status-badge"
        style={{ backgroundColor: statusColors[status] || '#95a5a6' }}
      >
        {status.toUpperCase()}
      </span>
    );
  };

  const getMeetingTypeIcon = (type) => {
    const icons = {
      'video': '📹',
      'phone': '📞',
      'in-person': '🏢',
      'audio': '🎤'
    };
    return icons[type] || '📅';
  };

  return (
    <div className="appointments-list-container">
      <div className="appointments-header">
        <h2>My Appointments</h2>
        <div className="filter-buttons">
          <button
            onClick={() => setFilter('all')}
            className={filter === 'all' ? 'active' : ''}
          >
            All
          </button>
          <button
            onClick={() => setFilter('upcoming')}
            className={filter === 'upcoming' ? 'active' : ''}
          >
            Upcoming
          </button>
        </div>
      </div>

      {error && (
        <div className="error-message">
          <span className="error-text">{error}</span>
          <button
            className="error-close"
            onClick={() => setError('')}
            aria-label="Close error"
          >
            ×
          </button>
        </div>
      )}

      {loading ? (
        <div className="appointments-skeleton">
          {[1, 2, 3].map(i => (
            <div key={i} className="appointment-card-skeleton">
              <Skeleton height={60} style={{ marginBottom: '15px' }} />
              <Skeleton height={20} count={3} style={{ marginBottom: '10px' }} />
              <Skeleton height={40} width="30%" />
            </div>
          ))}
        </div>
      ) : appointments.length === 0 ? (
        <div className="empty-state">
          <p>No appointments found.</p>
        </div>
      ) : (
        <div className="appointments-grid">
          {appointments.map((appointment) => (
            <div key={appointment.id} className="appointment-card">
              <div className="appointment-header">
                <div>
                  <h3>
                    {getMeetingTypeIcon(appointment.meetingType)} {' '}
                    {userType === 'user' ? (
                      <Link to={`/lawyer/${appointment.lawyerId}`} className="profile-link">
                        {appointment.lawyerFullName || 'Lawyer'}
                      </Link>
                    ) : (
                      appointment.userFullName || 'Client'
                    )}
                  </h3>
                  <p className="appointment-date">
                    {formatDateTime(appointment.appointmentDate)}
                  </p>
                </div>
                {getStatusBadge(appointment.status)}
              </div>

              <div className="appointment-details">
                <p><strong>Duration:</strong> {appointment.durationMinutes} minutes</p>
                <p><strong>Meeting Type:</strong> {appointment.meetingType}</p>
                {appointment.description && (
                  <p><strong>Description:</strong> {appointment.description}</p>
                )}
              </div>

              <div className="appointment-actions">
                {userType === 'user' &&
                  (appointment.status === 'pending' || appointment.status === 'confirmed') && (
                    <button
                      onClick={() => handleCancel(appointment.id)}
                      className="cancel-button"
                    >
                      Cancel
                    </button>
                  )}
                {userType === 'lawyer' && appointment.status === 'pending' && (
                  <button
                    onClick={() => handleConfirm(appointment.id)}
                    className="confirm-button"
                  >
                    Confirm
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default AppointmentsList;

