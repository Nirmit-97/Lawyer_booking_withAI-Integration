import axios from 'axios';
import { getToken } from './auth';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Add a request interceptor to include the auth token
api.interceptors.request.use(
    (config) => {
        const token = getToken();
        if (token) {
            config.headers['Authorization'] = `Bearer ${token.trim()}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Add a response interceptor to handle errors globally
api.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        const { removeToken } = require('./auth');

        if (error.response) {
            const status = error.response.status;

            if (status === 401) {
                console.warn(`Unauthorized (401) at ${error.config.url}. Clearing session.`);
                removeToken();
                // The AuthContext/ProtectedRoute will detect this change and redirect to login
            } else if (status === 403) {
                console.error(`Forbidden (403) at ${error.config.url}. Insufficient permissions.`);
                // DO NOT remove token on 403. Just notify the user.
            }
        } else if (error.code === 'ERR_NETWORK') {
            console.error('Network error - check your connection or backend status.');
        }

        return Promise.reject(error);
    }
);

export const audioApi = {
    upload: (formData, userId) => {
        // For FormData, let the browser set the Content-Type
        return api.post('/audio/upload', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
            params: userId ? { userId } : {},
        });
    },
    getAll: () => api.get('/audio/all'),
    getById: (id) => api.get(`/audio/${id}`),
};

export const casesApi = {
    create: (caseData) => api.post('/cases/create', caseData),
    getById: (id) => api.get(`/cases/${id}`),
    getByUser: (userId) => api.get(`/cases/user/${userId}`),
    getByLawyer: (lawyerId) => api.get(`/cases/lawyer/${lawyerId}`),
    getUnassigned: () => api.get('/cases/unassigned'),
    getRecommended: (lawyerId) => api.get(`/cases/recommended/${lawyerId}`),
    assignLawyer: (caseId, lawyerId) => api.post(`/cases/${caseId}/assign`, { lawyerId }),
    updateSolution: (caseId, solution) => api.put(`/cases/${caseId}/solution`, { solution }),
    updateStatus: (caseId, status) => api.put(`/cases/${caseId}/status`, { status }),
};

export const lawyersApi = {
    getProfile: (lawyerId) => api.get(`/lawyers/${lawyerId}/profile`),
    updateProfile: (lawyerId, profileData) => api.put(`/lawyers/${lawyerId}/profile`, profileData),
};

export const adminApi = {
    getStats: () => api.get('/admin/stats'),
    getUsers: (page = 0, size = 100) => api.get(`/admin/users?page=${page}&size=${size}`),
    getLawyers: (page = 0, size = 100) => api.get(`/admin/lawyers?page=${page}&size=${size}`),
    getCases: (page = 0, size = 100) => api.get(`/admin/cases?page=${page}&size=${size}`),
    deleteUser: (id) => api.delete(`/admin/users/${id}`),
    deleteLawyer: (id) => api.delete(`/admin/lawyers/${id}`),
    deleteCase: (id) => api.delete(`/admin/cases/${id}`),
    updateUser: (id, data) => api.put(`/admin/users/${id}`, data),
    updateLawyer: (id, data) => api.put(`/admin/lawyers/${id}`, data),
    updateCase: (id, data) => api.put(`/admin/cases/${id}`, data),
    reassignCase: (caseId, lawyerId) => api.put(`/admin/cases/${caseId}/reassign`, { lawyerId }),
};

export const messagesApi = {
    send: (messageData) => api.post('/messages/send', messageData),
    getByCase: (caseId) => api.get(`/messages/case/${caseId}`),
    markRead: (messageId) => api.put(`/messages/${messageId}/read`),
};

export default api;
