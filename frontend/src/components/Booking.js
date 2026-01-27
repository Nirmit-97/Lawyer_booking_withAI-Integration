import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import Skeleton from 'react-loading-skeleton';
import 'react-loading-skeleton/dist/skeleton.css';
import api from '../utils/api';
import './Booking.css';

function Booking({ userId, onBookingSuccess }) {
  const [lawyers, setLawyers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [formData, setFormData] = useState({
    lawyerId: '',
    appointmentDate: '',
    appointmentTime: '',
    durationMinutes: 60,
    meetingType: 'video',
    description: ''
  });

  useEffect(() => {
    fetchLawyers();
  }, []);

  const fetchLawyers = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/bookings/lawyers');
      setLawyers(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error('Error fetching lawyers:', err);
      const errorMsg = 'Error loading lawyers: Unable to connect to server';
      setError(errorMsg);
      toast.error(errorMsg);
      setLawyers([]);
    } finally {
      setLoading(false);
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
      // Combine date and time
      const dateTimeString = `${formData.appointmentDate}T${formData.appointmentTime}:00`;
      const appointmentDate = new Date(dateTimeString).toISOString();

      // Validate future date
      if (new Date(appointmentDate) <= new Date()) {
        setError('Appointment date must be in the future');
        setLoading(false);
        return;
      }

      const requestBody = {
        lawyerId: parseInt(formData.lawyerId),
        appointmentDate: appointmentDate,
        durationMinutes: parseInt(formData.durationMinutes),
        meetingType: formData.meetingType,
        description: formData.description
      };

      const response = await api.post('/bookings/create', requestBody, {
        headers: {
          'X-User-Id': userId.toString()
        }
      });

      if (response.data.success) {
        toast.success('Appointment booked successfully!');
        setFormData({
          lawyerId: '',
          appointmentDate: '',
          appointmentTime: '',
          durationMinutes: 60,
          meetingType: 'video',
          description: ''
        });
        if (onBookingSuccess) {
          onBookingSuccess();
        }
      } else {
        const errorMsg = response.data.message || 'Failed to book appointment';
        setError(errorMsg);
        toast.error(errorMsg);
      }
    } catch (err) {
      console.error('Error booking appointment:', err);
      setError('Error connecting to server. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Set minimum date to today
  const today = new Date().toISOString().split('T')[0];

  return (
    <div className="booking-container">
      <h2>Book an Appointment</h2>

      {error && !loading && (
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

      <form onSubmit={handleSubmit} className="booking-form">
        <div className="form-group">
          <label htmlFor="lawyerId">Select Lawyer *</label>
          {loading && lawyers.length === 0 ? (
            <Skeleton height={40} />
          ) : (
            <>
              <select
                id="lawyerId"
                name="lawyerId"
                value={formData.lawyerId}
                onChange={handleChange}
                required
                className="form-control"
              >
                <option value="">Choose a lawyer...</option>
                {lawyers.map(lawyer => (
                  <option key={lawyer.id} value={lawyer.id}>
                    {lawyer.fullName} - {lawyer.specialization}
                  </option>
                ))}
              </select>
              {formData.lawyerId && (
                <Link
                  to={`/lawyer/${formData.lawyerId}`}
                  target="_blank"
                  className="view-profile-link"
                >
                  View Lawyer Profile →
                </Link>
              )}
            </>
          )}
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="appointmentDate">Date *</label>
            <input
              type="date"
              id="appointmentDate"
              name="appointmentDate"
              value={formData.appointmentDate}
              onChange={handleChange}
              min={today}
              required
              className="form-control"
            />
          </div>

          <div className="form-group">
            <label htmlFor="appointmentTime">Time *</label>
            <input
              type="time"
              id="appointmentTime"
              name="appointmentTime"
              value={formData.appointmentTime}
              onChange={handleChange}
              required
              className="form-control"
            />
          </div>
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="durationMinutes">Duration (minutes) *</label>
            <select
              id="durationMinutes"
              name="durationMinutes"
              value={formData.durationMinutes}
              onChange={handleChange}
              required
              className="form-control"
            >
              <option value="30">30 minutes</option>
              <option value="60">1 hour</option>
              <option value="90">1.5 hours</option>
              <option value="120">2 hours</option>
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="meetingType">Meeting Type *</label>
            <select
              id="meetingType"
              name="meetingType"
              value={formData.meetingType}
              onChange={handleChange}
              required
              className="form-control"
            >
              <option value="video">Video Call</option>
              <option value="phone">Phone Call</option>
              <option value="in-person">In-Person</option>
              <option value="audio">Audio Only</option>
            </select>
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="description">Description / Reason for Appointment</label>
          <textarea
            id="description"
            name="description"
            value={formData.description}
            onChange={handleChange}
            rows="4"
            placeholder="Briefly describe the reason for your appointment..."
            className="form-control"
          />
        </div>

        {error && <div className="error-message">{error}</div>}

        <button type="submit" className="submit-button" disabled={loading}>
          {loading ? 'Booking...' : 'Book Appointment'}
        </button>
      </form>
    </div>
  );
}

export default Booking;

