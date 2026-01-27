import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const ProtectedRoute = ({ children, allowedRoles }) => {
    const { user, loading, isAuthenticated } = useAuth();
    const location = useLocation();

    if (loading) {
        return <div className="loading-spinner">Loading authentication...</div>;
    }

    if (!isAuthenticated) {
        // Redirect to appropriate login based on intended destination or default to user-login
        if (location.pathname.startsWith('/admin')) {
            return <Navigate to="/admin/login" state={{ from: location }} replace />;
        }
        if (location.pathname.startsWith('/lawyer-dashboard')) {
            return <Navigate to="/lawyer-login" state={{ from: location }} replace />;
        }
        return <Navigate to="/user-login" state={{ from: location }} replace />;
    }

    if (allowedRoles && !allowedRoles.includes(user.role)) {
        // User is authenticated but doesn't have the required role
        console.warn(`Access denied for role: ${user.role}. Required: ${allowedRoles}`);
        return <Navigate to="/" replace />; // Or an "Unauthorized" page
    }

    return children;
};

export default ProtectedRoute;
