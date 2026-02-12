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
    async (error) => {
        const { removeToken, getRefreshToken, setAccessToken } = require('./auth');
        const originalRequest = error.config;

        if (error.response) {
            const status = error.response.status;

            if (status === 401 && !originalRequest._retry) {
                originalRequest._retry = true;
                const refreshToken = getRefreshToken();

                if (refreshToken) {
                    try {
                        console.log('Access token expired. Attempting refresh...');
                        if (originalRequest.url.includes('/auth/refresh')) {
                            removeToken();
                            return Promise.reject(error);
                        }

                        const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
                            refreshToken: refreshToken
                        });

                        if (response.data && response.data.token) {
                            const newAccessToken = response.data.token;
                            const { login, getActiveRole, getSessionData } = require('./auth');

                            if (response.data.refreshToken) {
                                login(getActiveRole(), newAccessToken, getSessionData(), response.data.refreshToken);
                            } else {
                                setAccessToken(newAccessToken);
                            }

                            console.log('Token refreshed successfully.');

                            // Update header and retry
                            originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
                            return api(originalRequest);
                        }
                    } catch (refreshError) {
                        console.error('Session refresh failed:', refreshError);
                        removeToken();
                        return Promise.reject(refreshError);
                    }
                } else {
                    console.warn(`Unauthorized (401) at ${error.config.url}. No refresh token found.`);
                    removeToken();
                }
            } else if (status === 403) {
                console.error(`Forbidden (403) at ${error.config.url}. Insufficient permissions.`);
            }
        } else if (error.code === 'ERR_NETWORK') {
            console.error('Network error - check your connection or backend status.');
        }

        return Promise.reject(error);
    }
);

export const audioApi = {
    upload: (formData, userId) => {
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
    accept: (caseId) => api.post(`/cases/${caseId}/accept`),
    decline: (caseId) => api.post(`/cases/${caseId}/decline`),
    updateSolution: (caseId, solution) => api.put(`/cases/${caseId}/solution`, { solution }),
    updateStatus: (caseId, status) => api.put(`/cases/${caseId}/status`, { status }),
    saveDraft: (caseId, caseData) => api.put(`/cases/${caseId}/draft`, caseData),
    publish: (caseId, visibility) => api.put(`/cases/${caseId}/publish`, { visibility }),
    addFollowUp: (caseId, updateText) => api.post(`/cases/${caseId}/follow-up`, { updateText }),
    update: (caseId, caseData) => api.put(`/cases/${caseId}`, caseData),
};

export const lawyersApi = {
    getProfile: (lawyerId) => api.get(`/lawyers/${lawyerId}/profile`),
    updateProfile: (lawyerId, profileData) => api.put(`/lawyers/${lawyerId}/profile`, profileData),
    search: (params) => api.get('/lawyers/search', { params }),
};

export const documentsApi = {
    upload: (formData, caseId) => api.post('/documents/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
        params: { caseId }
    }),
    getByCase: (caseId) => api.get(`/documents/case/${caseId}`),
    download: (id) => `${API_BASE_URL}/documents/${id}/download`,
};

export const timelineApi = {
    getTimeline: (caseId) => api.get(`/cases/${caseId}/timeline`),
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
    verifyLawyer: (id, verified = true) => api.put(`/admin/lawyers/${id}/verify`, null, { params: { verified } }),
    getAppointments: (adminId) => api.get('/bookings/admin/all', {
        headers: { 'X-Admin-Id': adminId }
    }),
    getAuditLogs: (page = 0, size = 20) => api.get(`/admin/audit-logs?page=${page}&size=${size}`),
    getAnalytics: () => api.get('/admin/analytics'),
    getSettings: () => api.get('/admin/settings'),
    updateSetting: (setting) => api.post('/admin/settings', setting),
    deleteAppointment: (id, adminId) => api.delete(`/bookings/admin/${id}`, {
        headers: { 'X-Admin-Id': adminId }
    }),
};

export const reviewsApi = {
    submit: (reviewData) => api.post('/reviews', reviewData),
    getByLawyer: (lawyerId) => api.get(`/reviews/lawyer/${lawyerId}`),
};

export const messagesApi = {
    send: (messageData) => api.post('/messages/send', messageData),
    getByCase: (caseId) => api.get(`/messages/case/${caseId}`),
    markRead: (messageId) => api.put(`/messages/${messageId}/read`),
};

export const ttsApi = {
    generate: (caseId, language) => api.post('/tts/generate', { caseId, language })
};

export const offersApi = {
    submit: (caseId, offerData) => api.post(`/lawyer/offers/cases/${caseId}`, offerData),
    getMyOffers: () => api.get('/lawyer/offers/my-offers'),
    getForCase: (caseId) => api.get(`/user/cases/${caseId}/offers`),
    accept: (caseId, offerId) => api.post(`/user/cases/${caseId}/offers/${offerId}/accept`),
    withdraw: (offerId) => api.delete(`/lawyer/offers/${offerId}`),
};

export const paymentsApi = {
    create: (paymentData, idempotencyKey) => api.post('/payments/create', paymentData, {
        headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {}
    }),
    getStatus: (paymentId) => api.get(`/payments/${paymentId}`),
    verify: (verificationData) => api.post('/payments/verify', verificationData),
};

export default api;
