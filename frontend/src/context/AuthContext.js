import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import * as auth from '../utils/auth';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    const refreshUser = useCallback(() => {
        const userData = auth.getSessionData();
        const isAuthenticated = auth.isAuthenticated();
        const role = auth.getUserType();

        console.log(`[AuthContext] Refreshing user. Role: ${role}, Authenticated: ${isAuthenticated}`);

        if (isAuthenticated && userData && userData.username) {
            setUser({
                ...userData,
                role: role
            });
        } else {
            console.log(`[AuthContext] Session invalid or missing data for role ${role}. Clearing user.`);
            setUser(null);
        }
        setLoading(false);
    }, []);

    useEffect(() => {
        refreshUser();

        // Listen for storage events (multi-tab sync for tokens/data)
        const handleStorageChange = (e) => {
            const keysToTrack = ['token', 'adminData', 'userToken', 'userData', 'lawyerToken', 'lawyerData'];
            if (keysToTrack.includes(e.key)) {
                console.log(`[AuthContext] External storage change detected: ${e.key}. Refreshing...`);
                refreshUser();
            }
        };

        window.addEventListener('storage', handleStorageChange);
        return () => window.removeEventListener('storage', handleStorageChange);
    }, [refreshUser]);

    const login = (role, token, data, refreshToken) => {
        auth.login(role, token, data, refreshToken);
        refreshUser();
    };

    const logout = () => {
        auth.removeToken();
        setUser(null);
    };

    const value = {
        user,
        loading,
        login,
        logout,
        refreshUser,
        isAuthenticated: !!user,
        isAdmin: user?.role === 'admin',
        isLawyer: user?.role === 'lawyer',
        isUser: user?.role === 'user'
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
