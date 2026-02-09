import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Booking from './Booking';
import AppointmentsList from './AppointmentsList';
import CaseList from './CaseList';
import CaseDetail from './CaseDetail';
import AudioRecorder from './AudioRecorder';
import { casesApi } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import './Dashboard.css';
import { toast } from 'react-toastify';

function UserDashboard() {
  const [activeTab, setActiveTab] = useState('audio'); // 'audio', 'bookings', or 'cases'
  const { user, logout } = useAuth();
  const [cases, setCases] = useState([]);
  const [casesLoading, setCasesLoading] = useState(false);
  const [selectedCase, setSelectedCase] = useState(null);

  const navigate = useNavigate();
  const userId = user?.id ? parseInt(user.id) : null;

  // Safety return for unauthorized access (though ProtectedRoute handles this)
  // Moved early return to prevent hook violation
  // if (!user || user.role !== 'user') return null;

  const fetchCases = useCallback(async () => {
    if (!userId) return;
    setCasesLoading(true);
    try {
      const response = await casesApi.getByUser(userId);
      setCases(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error('Error fetching cases:', err);
      setCases([]);
      toast.error('Failed to load cases.');
    } finally {
      setCasesLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    if (userId && activeTab === 'cases') {
      fetchCases();
    }
  }, [userId, activeTab, fetchCases]);

  const handleLogout = () => {
    logout();
    navigate('/user-login');
  };

  const handleAudioUploadSuccess = (data) => {
    // If we are on cases tab, refresh
    if (activeTab === 'cases') {
      fetchCases();
    }
    // You can also automatically switch to cases tab if you want:
    // setActiveTab('cases');
  }

  // Listen for storage events to handle multi-tab session changes
  // Removed strict cross-tab logout enforcement to allow multi-role isolation

  // Safety return for unauthorized access (after hooks)
  if (!user || user.role !== 'user') return null;

  return (
    <div className="dashboard-container">
      <div className="dashboard-header">
        <h1>User Dashboard</h1>
        <div className="header-actions">
          <span className="username">Welcome, {user.username || 'User'}</span>
          <button onClick={handleLogout} className="logout-button">Logout</button>
        </div>
      </div>

      <div className="tab-navigation">
        <button
          className={activeTab === 'audio' ? 'tab-button active' : 'tab-button'}
          onClick={() => { setActiveTab('audio'); setSelectedCase(null); }}
        >
          🎤 Audio Processing
        </button>
        <button
          className={activeTab === 'bookings' ? 'tab-button active' : 'tab-button'}
          onClick={() => { setActiveTab('bookings'); setSelectedCase(null); }}
        >
          📅 Bookings
        </button>
        <button
          className={activeTab === 'cases' ? 'tab-button active' : 'tab-button'}
          onClick={() => {
            setCases([]); // RESET STATE immediately
            setActiveTab('cases');
            setSelectedCase(null);
          }}
        >
          📋 My Cases
        </button>
      </div>

      <div className="dashboard-content">
        {activeTab === 'audio' && (
          <AudioRecorder userId={userId} onUploadSuccess={handleAudioUploadSuccess} />
        )}

        {activeTab === 'bookings' && (
          <div className="bookings-tab-content">
            {userId && (
              <>
                <Booking userId={userId} onBookingSuccess={() => { }} />
                <AppointmentsList userId={userId} userType="user" />
              </>
            )}
          </div>
        )}

        {activeTab === 'cases' && (
          <div className="cases-tab-content">
            {selectedCase ? (
              <CaseDetail
                caseId={selectedCase.id}
                userType="user"
                userId={userId}
                onBack={() => { setSelectedCase(null); fetchCases(); }} // Refresh list on back
              />
            ) : (
              <>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
                  <h2>My Cases</h2>
                  <button onClick={fetchCases} className="refresh-button">Refresh</button>
                </div>
                {casesLoading ? <p>Loading cases...</p> : (
                  <CaseList
                    cases={cases}
                    onSelectCase={setSelectedCase}
                    userType="user"
                  />
                )}
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default UserDashboard;

