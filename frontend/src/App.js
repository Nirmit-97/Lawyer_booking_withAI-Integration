import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import LandingPage from './components/LandingPage';
import UserLogin from './components/UserLogin';
import UserRegistration from './components/UserRegistration';
import LawyerLogin from './components/LawyerLogin';
import LawyerRegistration from './components/LawyerRegistration';
import UserDashboard from './components/UserDashboard';
import LawyerDashboard from './components/LawyerDashboard';
import LawyerProfile from './components/LawyerProfile';
import AdminLogin from './components/AdminLogin';
import AdminDashboard from './components/AdminDashboard';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './App.css';

import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
  return (
    <AuthProvider>
      <Router>
        <div className="App">
          <ToastContainer />
          <Routes>
            {/* Public Routes */}
            <Route path="/" element={<LandingPage />} />
            <Route path="/user-login" element={<UserLogin />} />
            <Route path="/user-register" element={<UserRegistration />} />
            <Route path="/lawyer-login" element={<LawyerLogin />} />
            <Route path="/lawyer-register" element={<LawyerRegistration />} />
            <Route path="/admin/login" element={<AdminLogin />} />

            {/* Protected User Routes */}
            <Route path="/user-dashboard" element={
              <ProtectedRoute allowedRoles={['user']}>
                <UserDashboard />
              </ProtectedRoute>
            } />

            {/* Protected Lawyer Routes */}
            <Route path="/lawyer-dashboard" element={
              <ProtectedRoute allowedRoles={['lawyer']}>
                <LawyerDashboard />
              </ProtectedRoute>
            } />
            <Route path="/lawyer/:id" element={<LawyerProfile />} /> {/* Profile might be public-viewable but edit-protected? For now keeping it simple. */}

            {/* Protected Admin Routes */}
            <Route path="/admin/dashboard" element={
              <ProtectedRoute allowedRoles={['admin']}>
                <AdminDashboard />
              </ProtectedRoute>
            } />
          </Routes>
        </div>
      </Router>
    </AuthProvider>
  );
}

export default App;

