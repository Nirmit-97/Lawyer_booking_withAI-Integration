import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import useLawyerSocket from '../hooks/useLawyerSocket';
import AppointmentsList from './AppointmentsList';
import CaseList from './CaseList';
import CaseDetail from './CaseDetail';
import LawyerProfile from './LawyerProfile';
import { casesApi, audioApi, lawyersApi, ttsApi } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import { toast } from 'react-toastify';

function LawyerDashboard() {
    const [records, setRecords] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [playingRecordId, setPlayingRecordId] = useState(null);
    const [currentAudioUrl, setCurrentAudioUrl] = useState(null);
    const [selectedLanguage, setSelectedLanguage] = useState({}); // { recordId: 'en' | 'gu' }
    const [activeTab, setActiveTab] = useState('audio'); // 'audio', 'appointments', 'cases', or 'profile'

    const { user, logout } = useAuth();
    const lawyerId = user?.id ? parseInt(user.id) : null;

    const [cases, setCases] = useState([]);
    const [unassignedCases, setUnassignedCases] = useState([]);
    const [casesLoading, setCasesLoading] = useState(false);
    const [selectedCase, setSelectedCase] = useState(null);
    const [schedFilter, setSchedFilter] = useState('all');
    const [creatingCaseId, setCreatingCaseId] = useState(null);
    const [lawyerProfile, setLawyerProfile] = useState(null);
    const lawyerProfileRef = useRef(null);
    const [broadcastMatches, setBroadcastMatches] = useState([]); // General pool matching specializations
    const [activeRequests, setActiveRequests] = useState([]); // Direct connection requests (PENDING_APPROVAL)
    const [pipelineCases, setPipelineCases] = useState([]); // UNDER_REVIEW, PAYMENT_PENDING
    const [historyCases, setHistoryCases] = useState([]); // CLOSED
    const audioRef = useRef(null);
    const navigate = useNavigate();

    // Safety return for unauthorized access (though ProtectedRoute handles this)
    // Safety return moved to bottom to prevent Hook errors


    const handleLogout = useCallback(() => {
        // Stop any playing audio before logout
        if (audioRef.current) {
            audioRef.current.pause();
            audioRef.current = null;
        }
        if (currentAudioUrl) {
            URL.revokeObjectURL(currentAudioUrl);
            setCurrentAudioUrl(null);
        }
        setPlayingRecordId(null);
        logout();
        navigate('/lawyer-login');
    }, [currentAudioUrl, navigate, logout]);

    const fetchLawyerProfile = useCallback(async (id) => {
        const targetId = id || lawyerId;
        if (!targetId) return;
        try {
            const response = await lawyersApi.getProfile(targetId);
            setLawyerProfile(response.data);
            lawyerProfileRef.current = response.data; // Update ref for WS
            console.log('Lawyer profile loaded:', response.data);
        } catch (err) {
            console.error('Error fetching lawyer profile:', err);
        }
    }, [lawyerId]);

    const fetchCases = useCallback(async () => {
        if (!lawyerId) return;
        setCasesLoading(true);
        try {
            // 1. Fetch cases assigned to this lawyer (fetch first for filtering)
            const assignedResponse = await casesApi.getByLawyer(lawyerId);
            const allAssigned = Array.isArray(assignedResponse.data) ? assignedResponse.data : [];

            // Categorize into Requests (PENDING_APPROVAL) vs Pipeline vs Active (IN_PROGRESS) vs History (CLOSED)
            const requests = allAssigned.filter(c => c.caseStatus?.toUpperCase() === 'PENDING_APPROVAL');
            const pipeline = allAssigned.filter(c => c.caseStatus?.toUpperCase() === 'UNDER_REVIEW' || c.caseStatus?.toUpperCase() === 'PAYMENT_PENDING');
            const active = allAssigned.filter(c => c.caseStatus?.toUpperCase() === 'IN_PROGRESS');
            const history = allAssigned.filter(c => c.caseStatus?.toUpperCase() === 'CLOSED');

            setActiveRequests(requests);
            setPipelineCases(pipeline);
            setCases(active);
            setHistoryCases(history);

            // Create a set of IDs already assigned to this lawyer for filtering
            const myCaseIds = new Set(allAssigned.map(c => c.id));

            // 2. Fetch unassigned cases (Broadcast matches)
            const unassignedResponse = await casesApi.getRecommended(lawyerId);
            const rawRecommended = Array.isArray(unassignedResponse.data) ? unassignedResponse.data : [];

            // Filter out any cases that might already be assigned locally (safety)
            const recommended = rawRecommended.filter(c => !myCaseIds.has(c.id));
            setUnassignedCases(recommended);

            // Cleanup local matching notifications
            const unassignedIds = new Set(recommended.map(c => c.id));
            setBroadcastMatches(prev => prev.filter(req => !unassignedIds.has(req.caseId) && !myCaseIds.has(req.caseId)));

        } catch (err) {
            console.error('Error fetching cases:', err);
            if (err.response?.status !== 401) {
                console.error('Failed to load cases:', err.response?.data || err.message);
                toast.error('Failed to load cases: ' + (err.response?.data?.message || err.message));
            }
        } finally {
            setCasesLoading(false);
        }
    }, [lawyerId]);

    const fetchRecords = useCallback(async () => {
        setLoading(true);
        setError('');

        try {
            const response = await audioApi.getAll();
            setRecords(Array.isArray(response.data) ? response.data : []);
        } catch (err) {
            setError('Error fetching records: ' + err.message);
            console.error('Error fetching records:', err);
            setRecords([]);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (lawyerId) {
            fetchLawyerProfile(lawyerId);
        }
    }, [lawyerId, fetchLawyerProfile]);


    // --- WebSocket Logic (Using Custom Hook) ---
    const handleNewRequest = useCallback((payload) => {
        setBroadcastMatches(prev => {
            if (prev.some(r => r.caseId === payload.caseId)) return prev;
            return [payload, ...prev];
        });

        toast.info(
            <div>
                <strong>🆕 New Match: {payload.title}</strong>
                <p style={{ fontSize: '0.85rem', margin: '5px 0', color: '#666' }}>
                    A new case matching your expertise is available in the pool.
                </p>
            </div>,
            {
                position: "top-right",
                autoClose: 10000,
                onClick: () => {
                    setActiveTab('cases');
                    fetchCases();
                }
            }
        );
        fetchCases();
    }, [fetchCases]);

    const handleCaseAssigned = useCallback((caseId) => {
        // 1. Remove from local unassigned/pending lists
        setUnassignedCases(prev => prev.filter(c => c.id !== caseId));
        setBroadcastMatches(prev => prev.filter(r => r.caseId !== caseId));

        // 2. Refresh My Cases and Records
        fetchCases();
        fetchRecords();
    }, [fetchCases, fetchRecords]);

    const handleCaseDeleted = useCallback((caseId) => {
        console.log('WS CASE DELETED:', caseId);
        // Remove from ALL lists
        setCases(prev => prev.filter(c => c.id !== caseId));
        setActiveRequests(prev => prev.filter(c => c.id !== caseId));
        setPipelineCases(prev => prev.filter(c => c.id !== caseId));
        setHistoryCases(prev => prev.filter(c => c.id !== caseId));
        setUnassignedCases(prev => prev.filter(c => c.id !== caseId));
        setBroadcastMatches(prev => prev.filter(r => r.caseId !== caseId));

        // If selected case was deleted, clear selection
        setSelectedCase(prev => (prev && prev.id === caseId) ? null : prev);

        // REFRESH AUDIO RECORDS
        fetchRecords();

        toast.info("A case has been deleted by Admin.");
    }, [fetchRecords]);

    useLawyerSocket({
        lawyerId,
        lawyerProfileRef,
        onNewRequest: handleNewRequest,
        onCaseAssigned: handleCaseAssigned,
        onCaseDeleted: handleCaseDeleted,
        onCaseUpdated: (caseId) => {
            console.log('WS CASE UPDATED:', caseId);
            fetchCases();
        }
    });
    // -------------------------------------------

    // Fetch initial data based on tab
    useEffect(() => {
        if (activeTab === 'audio') fetchRecords();
        if (activeTab === 'cases' && lawyerId) fetchCases();
    }, [activeTab, lawyerId, fetchRecords, fetchCases]);

    const handleBidOnCase = useCallback((caseId) => {
        // Find the case in local lists or create a placeholder
        const caseItem = unassignedCases.find(c => c.id === caseId) ||
            broadcastMatches.find(r => r.caseId === caseId) ||
            { id: caseId };

        setSelectedCase(caseItem);
        setActiveTab('cases');
        toast.info('Please submit your offer to connect with the client.');
    }, [unassignedCases, broadcastMatches]);


    const handleDecline = async (caseId) => {
        try {
            await casesApi.decline(caseId);
            toast.success('Case request declined.');
            fetchCases();
        } catch (err) {
            console.error('Error declining case:', err);
            toast.error('Failed to decline case request');
        }
    };

    const createCaseFromAudio = useCallback(async (record) => {
        if (!lawyerId) return;

        // Check for userId in the record
        const targetUserId = record.userId || record.user_id;

        if (!targetUserId) {
            toast.error("Cannot create case: Audio record is missing User ID (Backend update required).");
            return;
        }

        setCreatingCaseId(record.id);
        try {
            // 1. Create Case
            const description = (record.maskedEnglishText || record.maskedGujaratiText || "Audio Record Case") + "\n\n(Generated from Audio Record #" + record.id + ")";
            const caseData = {
                userId: targetUserId,
                caseTitle: `Case from Audio #${record.id}`,
                caseType: null,
                description: description
            };

            const createResponse = await casesApi.create(caseData);
            const newCase = createResponse.data || createResponse;

            if (!newCase || !newCase.id) throw new Error("Failed to create case");

            toast.success("Case initialized successfully! It will be visible for offers once the user publishes it.");
            const caseId = newCase.id;

            // 3. Clear from pools immediately
            setUnassignedCases(prev => prev.filter(c => c.id !== caseId));
            setBroadcastMatches(prev => prev.filter(r => r.caseId !== caseId));

            // 4. Refresh and switch view
            await fetchRecords();
            await fetchCases();
            setActiveTab('cases');
            setSelectedCase(newCase);

        } catch (err) {
            console.error("Error creating case from audio:", err);
            toast.error("Failed to create case: " + (err.response?.data?.message || err.message));
        } finally {
            setCreatingCaseId(null);
        }
    }, [lawyerId, fetchRecords, fetchCases]);





    // Cleanup audio on unmount
    useEffect(() => {
        return () => {
            if (audioRef.current) {
                audioRef.current.pause();
                audioRef.current = null;
            }
        };
    }, []);

    const playAudio = async (recordId, language) => {
        // Toggle logic
        if (playingRecordId === recordId && audioRef.current) {
            if (!audioRef.current.paused) {
                audioRef.current.pause();
                setPlayingRecordId(null);
                return;
            }
            audioRef.current.play().catch(console.error);
            return;
        }

        if (audioRef.current) {
            audioRef.current.pause();
        }
        if (currentAudioUrl) {
            URL.revokeObjectURL(currentAudioUrl);
        }

        // Show loading state (you might want to add a specific loading state for audio)
        // For now, we'll just set playingRecordId to indicate activity? 
        // Or better, let's add a local loading state if needed. 
        // But re-using existing state structure:

        try {
            // Determine caseId. 
            // The record object has caseId. We need to find the record first to get caseId if not passed.
            // Actually, we can pass the whole record or just IDs. 
            // Looking at usage: playAudio(audioData, record.id)
            // We need to change usage to: playAudio(record)

            // Wait, I need to fetch the audio first.
            const record = records.find(r => r.id === recordId);
            if (!record) return;

            const caseId = record.caseId;
            // Note: If no caseId (e.g. just audio upload without case?), we might need another way.
            // But usually records are tied to cases or users. 
            // If it's a raw audio record, maybe we use the text? 
            // The TTS API expects caseId. 

            // IF record has cached audio bytes, we can use them directly?
            // The TTS API handles caching. So calling it is safe and efficient.
            // But we need caseId.

            if (!caseId) {
                // Fallback or error if no case ID. 
                // If the record exists but no case, maybe we can't generate specific case audio?
                // Text is in the record. 
                // Let's assume for now we use the API which takes caseId.
                // If no caseId, we might fail.
                if (!record.caseId) {
                    toast.error("Case ID missing. Cannot generate audio.");
                    return;
                }
            }

            // Fetch Audio
            const response = await ttsApi.generate(caseId, language);
            const base64Audio = response.data.audio;

            // Play
            const audioBlob = await (await fetch(`data:audio/mp3;base64,${base64Audio}`)).blob();
            const url = URL.createObjectURL(audioBlob);
            setCurrentAudioUrl(url);

            const audio = new Audio(url);
            audioRef.current = audio;
            setPlayingRecordId(recordId);

            audio.play().catch(e => {
                console.error("Playback error:", e);
                toast.error('Playback error: ' + e.message);
                setPlayingRecordId(null);
            });
            audio.onended = () => setPlayingRecordId(null);

        } catch (err) {
            console.error('Error getting/playing audio:', err);
            toast.error('Failed to play audio');
            setPlayingRecordId(null);
        }
    };

    const handlePlayCaseAudio = async (caseId, language = 'en') => {
        if (audioRef.current) {
            audioRef.current.pause();
        }
        if (currentAudioUrl) {
            URL.revokeObjectURL(currentAudioUrl);
            setCurrentAudioUrl(null);
        }

        const langName = language === 'en' ? 'English' : 'Gujarati';
        toast.loading(`Loading ${langName} case audio...`, { toastId: 'tts-loading' });
        try {
            const response = await ttsApi.generate(caseId, language);
            const base64Audio = response.data.audio;
            const audioBlob = await (await fetch(`data:audio/mp3;base64,${base64Audio}`)).blob();
            const url = URL.createObjectURL(audioBlob);

            setCurrentAudioUrl(url);
            const audio = new Audio(url);
            audioRef.current = audio;
            audio.onended = () => {
                URL.revokeObjectURL(url);
                setCurrentAudioUrl(null);
            };
            audio.play().catch(e => console.error("Playback error:", e));
            toast.dismiss('tts-loading');
        } catch (error) {
            console.error('Error generating audio:', error);
            toast.dismiss('tts-loading');
            toast.error('Failed to generate case audio.');
        }
    };

    // Safety return for unauthorized access (though ProtectedRoute handles this)
    if (!user || user.role !== 'lawyer') return null;

    return (
        <div className="dashboard-container min-h-screen bg-background-light dark:bg-background-dark p-6 lg:p-12 font-display">
            <div className="dashboard-header glass-card p-8 rounded-3xl mb-8 flex justify-between items-center border border-white/50 shadow-xl">
                <div>
                    <h1 className="text-3xl font-black tracking-tight text-primary dark:text-white">Expert Terminal</h1>
                    <p className="text-sm text-electric-blue font-black uppercase tracking-[0.2em] mt-1">Legal Intelligence Access</p>
                </div>
                <div className="header-actions flex items-center gap-6">
                    <span className="username font-bold text-gray-500">
                        {user.fullName || user.username || 'Legal Expert'}
                    </span>
                    <button
                        onClick={() => setActiveTab('profile')}
                        className="flex items-center gap-2 text-sm font-black uppercase tracking-widest text-primary hover:text-electric-blue transition-colors"
                    >
                        <span className="material-symbols-outlined">account_circle</span>
                        Profile
                    </button>
                    <button onClick={handleLogout} className="flex items-center justify-center rounded-full h-11 px-6 border-2 border-red-500/20 text-red-500 text-sm font-black uppercase tracking-widest hover:bg-red-500 hover:text-white transition-all">
                        Logout
                    </button>
                </div>
            </div>

            <div className="tab-navigation flex gap-4 mb-8">
                <button
                    className={`flex items-center gap-2 px-8 py-4 rounded-2xl font-black text-sm uppercase tracking-widest transition-all ${activeTab === 'audio'
                        ? 'bg-electric-blue text-white shadow-lg shadow-electric-blue/30 scale-105'
                        : 'glass-card text-gray-500 hover:bg-white'
                        }`}
                    onClick={() => { setActiveTab('audio'); setSelectedCase(null); }}
                >
                    <span className="material-symbols-outlined">psychology</span>
                    Intelligence Pool
                </button>
                <button
                    className={`flex items-center gap-2 px-8 py-4 rounded-2xl font-black text-sm uppercase tracking-widest transition-all ${activeTab === 'appointments'
                        ? 'bg-electric-blue text-white shadow-lg shadow-electric-blue/30 scale-105'
                        : 'glass-card text-gray-500 hover:bg-white'
                        }`}
                    onClick={() => { setActiveTab('appointments'); setSelectedCase(null); }}
                >
                    <span className="material-symbols-outlined">calendar_today</span>
                    Schedule
                </button>
                <button
                    className={`flex items-center gap-2 px-8 py-4 rounded-2xl font-black text-sm uppercase tracking-widest transition-all relative ${activeTab === 'cases'
                        ? 'bg-electric-blue text-white shadow-lg shadow-electric-blue/30 scale-105'
                        : 'glass-card text-gray-500 hover:bg-white'
                        }`}
                    onClick={() => {
                        setActiveTab('cases');
                        setSelectedCase(null);
                        fetchCases();
                    }}
                >
                    <span className="material-symbols-outlined">folder_shared</span>
                    Case Queue
                    {(activeRequests.length + pipelineCases.length + broadcastMatches.length) > 0 && (
                        <span className="absolute -top-2 -right-2 bg-red-500 text-white text-[10px] font-black w-6 h-6 flex items-center justify-center rounded-full shadow-lg border-2 border-white">
                            {activeRequests.length + pipelineCases.length + broadcastMatches.length}
                        </span>
                    )}
                </button>
                <button
                    className={`flex items-center gap-2 px-8 py-4 rounded-2xl font-black text-sm uppercase tracking-widest transition-all ${activeTab === 'profile'
                        ? 'bg-electric-blue text-white shadow-lg shadow-electric-blue/30 scale-105'
                        : 'glass-card text-gray-500 hover:bg-white'
                        }`}
                    onClick={() => { setActiveTab('profile'); setSelectedCase(null); }}
                >
                    <span className="material-symbols-outlined">security</span>
                    Secure Profile
                </button>
            </div>

            <div className="dashboard-content">
                {user?.role === 'lawyer' && lawyerProfile && !lawyerProfile.verified && (
                    <div className="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-2xl p-6 mb-8 flex items-center gap-6 animate-in fade-in slide-in-from-top-4 duration-500">
                        <div className="bg-amber-100 dark:bg-amber-900/40 p-3 rounded-xl text-amber-600 dark:text-amber-400">
                            <span className="material-symbols-outlined text-3xl animate-pulse">verified_user</span>
                        </div>
                        <div className="flex-1">
                            <h3 className="text-lg font-black text-amber-900 dark:text-amber-100 uppercase tracking-tight">Profile Verification Pending</h3>
                            <p className="text-sm font-medium text-amber-700 dark:text-amber-300 mt-1">Your profile is currently under review by our admin team. While you can browse cases, you will not be able to accept connections until your credentials are verified.</p>
                        </div>
                        <button
                            onClick={() => setActiveTab('profile')}
                            className="px-6 py-3 bg-amber-600 hover:bg-amber-700 text-white rounded-xl text-xs font-black uppercase tracking-widest transition-all shadow-lg shadow-amber-600/20"
                        >
                            Complete Profile
                        </button>
                    </div>
                )}

                {activeTab === 'audio' && (
                    <div className="records-section">
                        <div className="flex justify-between items-center mb-8">
                            <h2 className="text-2xl font-black text-primary uppercase tracking-tight">Intelligence Pool</h2>
                            <button onClick={fetchRecords} className="flex items-center gap-2 text-sm font-black uppercase tracking-widest text-electric-blue hover:underline" disabled={loading}>
                                <span className="material-symbols-outlined">sync</span>
                                {loading ? 'Syncing...' : 'Sync Pool'}
                            </button>
                        </div>

                        {error && <div className="error-message">{error}</div>}

                        {loading ? (
                            <div className="loading-message">Loading records...</div>
                        ) : records.length === 0 ? (
                            <div className="empty-state">
                                <p>No records found.</p>
                            </div>
                        ) : (
                            <div className="records-grid grid grid-cols-1 xl:grid-cols-2 gap-8">
                                {records
                                    .filter(record => !record.deleted && !record.lawyerId)
                                    .map((record) => (
                                        <div key={record.id} className="record-card glass-card p-8 rounded-3xl border border-white/50 shadow-xl hover-lift">
                                            <div className="record-header flex justify-between items-center border-b border-gray-100 pb-4 mb-6">
                                                <h3 className="text-xl font-black text-primary tracking-tight">
                                                    {record.caseTitle || `Log Identification: #${record.id}`}
                                                </h3>
                                                <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-electric-blue/10 text-electric-blue text-[10px] font-black uppercase">
                                                    <span className="material-symbols-outlined !text-sm">language</span>
                                                    {record.language || 'N/A'}
                                                </div>
                                            </div>

                                            <div className="record-content flex flex-col gap-6">
                                                <div className="record-field">
                                                    <p className="text-[10px] font-black text-gray-500 uppercase tracking-widest mb-3">Linguistic Matrix</p>
                                                    <div className="flex gap-2">
                                                        {['en', 'gu'].map(lang => (
                                                            <button
                                                                key={lang}
                                                                onClick={() => setSelectedLanguage({ ...selectedLanguage, [record.id]: lang })}
                                                                className={`px-4 py-2 rounded-xl text-xs font-black uppercase tracking-widest transition-all ${(selectedLanguage[record.id] || 'en') === lang
                                                                    ? 'bg-electric-blue text-white shadow-lg shadow-electric-blue/20'
                                                                    : 'bg-gray-100 text-gray-400 hover:bg-white border border-transparent hover:border-gray-200'
                                                                    }`}
                                                            >
                                                                {lang === 'en' ? 'English' : 'Gujarati'}
                                                            </button>
                                                        ))}
                                                    </div>
                                                </div>

                                                <div className="record-field">
                                                    <p className="text-[10px] font-black text-gray-500 uppercase tracking-widest mb-3">Intelligence Preview</p>
                                                    <div className="glass-ai p-4 rounded-xl text-sm bg-white/50 border border-white text-primary leading-relaxed min-h-[80px]">
                                                        {selectedLanguage[record.id] === 'gu'
                                                            ? (record.maskedGujaratiText || 'N/A')
                                                            : (record.maskedEnglishText || 'N/A')}
                                                    </div>
                                                </div>

                                                <div className="record-field pt-4 border-t border-gray-100 mt-2">
                                                    <div className="flex items-center justify-between gap-4">
                                                        {(() => {
                                                            // Removed check for existing audio data as we now fetch on demand
                                                            // But we might want to check if text exists?
                                                            // For now, show button if record exists.
                                                            const isGujarati = selectedLanguage[record.id] === 'gu';
                                                            // const audioData = ... (Removed)

                                                            return (
                                                                <button
                                                                    onClick={() => playAudio(record.id, selectedLanguage[record.id] || 'en')}
                                                                    className="flex items-center justify-center gap-2 rounded-full h-11 px-6 bg-primary text-white text-xs font-black uppercase tracking-widest hover-lift"
                                                                >
                                                                    <span className="material-symbols-outlined !text-lg">
                                                                        {playingRecordId === record.id ? 'pause' : 'play_arrow'}
                                                                    </span>
                                                                    {playingRecordId === record.id ? 'Pause' : 'Playback'}
                                                                </button>
                                                            );
                                                        })()}

                                                        <button
                                                            className={`flex items-center justify-center rounded-full h-11 px-6 text-xs font-black uppercase tracking-widest transition-all ${record.lawyerId || (lawyerProfile && !lawyerProfile.verified)
                                                                ? 'bg-gray-100 text-gray-400 cursor-not-allowed opacity-60'
                                                                : 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/20 hover-lift'
                                                                }`}
                                                            disabled={!!record.lawyerId || creatingCaseId === record.id || (lawyerProfile && !lawyerProfile.verified)}
                                                            onClick={() => record.caseId ? handleBidOnCase(record.caseId) : createCaseFromAudio(record)}
                                                            title={lawyerProfile && !lawyerProfile.verified ? "Verification Required" : ""}
                                                        >
                                                            {creatingCaseId === record.id ? 'Synchronizing...' :
                                                                record.lawyerId ? 'Expert Assigned' :
                                                                    lawyerProfile && !lawyerProfile.verified ? 'Verification Required' :
                                                                        record.caseId ? 'Submit Quote' : 'Initialize Case'}
                                                        </button>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'appointments' && (
                    <div className="appointments-tab-content animate-in fade-in slide-in-from-bottom-4 duration-700">
                        <div className="flex flex-col md:flex-row md:items-center justify-between mb-8 gap-4">
                            <div>
                                <h2 className="text-3xl font-bold text-slate-900 dark:text-white">Professional Schedule</h2>
                                <p className="text-slate-500 dark:text-slate-400 mt-1 text-sm font-medium">Manage your upcoming legal consultations and client engagements.</p>
                            </div>
                            <div className="flex bg-white dark:bg-slate-800 p-1 rounded-lg border border-slate-200 dark:border-slate-700 shadow-sm w-fit">
                                <button
                                    onClick={() => setSchedFilter('all')}
                                    className={`px-6 py-1.5 text-xs font-black rounded-md transition-all uppercase tracking-widest ${schedFilter === 'all' ? 'bg-slate-900 dark:bg-white text-white dark:text-slate-900' : 'text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200'
                                        }`}
                                >
                                    ALL
                                </button>
                                <button
                                    onClick={() => setSchedFilter('upcoming')}
                                    className={`px-6 py-1.5 text-xs font-bold transition-all uppercase tracking-widest ${schedFilter === 'upcoming' ? 'bg-slate-900 dark:bg-white text-white dark:text-slate-900' : 'text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200'
                                        }`}
                                >
                                    UPCOMING
                                </button>
                            </div>
                        </div>
                        {lawyerId ? (
                            <AppointmentsList userId={lawyerId} userType="lawyer" externalFilter={schedFilter} />
                        ) : (
                            <div className="p-8 text-center text-rose-500 font-bold uppercase tracking-widest text-xs bg-rose-50 dark:bg-rose-950/20 rounded-2xl border border-rose-100 dark:border-rose-900/40">
                                Expert ID not established.
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'cases' && (
                    <div className="cases-tab-content animate-in fade-in slide-in-from-bottom-4 duration-700">
                        {selectedCase ? (
                            <CaseDetail
                                caseId={selectedCase.id}
                                userType="lawyer"
                                userId={lawyerId}
                                onBack={() => { setSelectedCase(null); fetchCases(); }}
                            />
                        ) : (
                            <div className="grid grid-cols-12 gap-8">
                                {/* Left Column: Workflows & Discovery */}
                                <div className="col-span-12 lg:col-span-7 space-y-8">
                                    {/* Direct Requests Section */}
                                    <section className="bg-white dark:bg-surface-dark rounded-xl border border-slate-200/50 dark:border-slate-700/50 overflow-hidden shadow-sm">
                                        <div className="p-6 border-b border-rose-500/20 bg-rose-50/30 dark:bg-rose-900/10">
                                            <div className="flex items-center gap-2 text-rose-600 dark:text-rose-400">
                                                <span className="material-symbols-outlined text-xl">warning</span>
                                                <h2 className="font-bold uppercase tracking-wide text-sm">Direct Requests</h2>
                                            </div>
                                            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">Immediate requests requiring your approval.</p>
                                        </div>

                                        {activeRequests.length === 0 ? (
                                            <div className="p-12 flex flex-col items-center justify-center text-center">
                                                <div className="w-16 h-16 bg-slate-100 dark:bg-slate-800 rounded-full flex items-center justify-center mb-4">
                                                    <span className="material-symbols-outlined text-slate-400 text-3xl">folder_off</span>
                                                </div>
                                                <h3 className="text-sm font-bold text-slate-400 uppercase tracking-widest">No Intelligence Records Found</h3>
                                            </div>
                                        ) : (
                                            <div className="overflow-x-auto">
                                                <CaseList
                                                    cases={activeRequests}
                                                    userType="lawyer"
                                                    onDecline={handleDecline}
                                                    onSelectCase={setSelectedCase}
                                                />
                                            </div>
                                        )}
                                    </section>

                                    {/* Pipeline & Payments Section */}
                                    <section className="bg-white dark:bg-surface-dark rounded-xl border border-slate-200/50 dark:border-slate-700/50 overflow-hidden shadow-sm">
                                        <div className="p-6 border-b border-amber-500/20 bg-amber-50/30 dark:bg-amber-900/10">
                                            <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400">
                                                <span className="material-symbols-outlined text-xl">payments</span>
                                                <h2 className="font-bold uppercase tracking-wide text-sm">Engagement Pipeline</h2>
                                            </div>
                                            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">Offers sent or awaiting payment to start work.</p>
                                        </div>

                                        {pipelineCases.length === 0 ? (
                                            <div className="p-12 flex flex-col items-center justify-center text-center">
                                                <div className="w-16 h-16 bg-slate-100 dark:bg-slate-800 rounded-full flex items-center justify-center mb-4">
                                                    <span className="material-symbols-outlined text-slate-400 text-3xl">hourglass_empty</span>
                                                </div>
                                                <h3 className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">No cases in pipeline</h3>
                                            </div>
                                        ) : (
                                            <div className="overflow-x-auto">
                                                <CaseList
                                                    cases={pipelineCases}
                                                    userType="lawyer"
                                                    onSelectCase={setSelectedCase}
                                                />
                                            </div>
                                        )}
                                    </section>

                                    {/* Discovery Pool Section */}
                                    <section className="bg-white dark:bg-surface-dark rounded-xl border border-slate-200/50 dark:border-slate-700/50 overflow-hidden shadow-sm">
                                        <div className="p-6 border-b border-primary/20 bg-primary/5 dark:bg-primary/10">
                                            <div className="flex items-center gap-2 text-primary">
                                                <span className="material-symbols-outlined text-xl">campaign</span>
                                                <h2 className="font-bold uppercase tracking-wide text-sm">Discovery Pool</h2>
                                            </div>
                                            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">Broadcasted cases matching your specializations.</p>
                                        </div>

                                        <div className="overflow-x-auto">
                                            <CaseList
                                                cases={[
                                                    ...broadcastMatches
                                                        .filter(r => !cases.some(c => c.id === r.caseId) && !activeRequests.some(c => c.id === r.caseId))
                                                        .map(r => ({
                                                            ...r,
                                                            id: r.caseId,
                                                            caseTitle: r.title,
                                                            caseCategory: r.category,
                                                            caseStatus: 'MATCHED'
                                                        })),
                                                    ...unassignedCases.filter(u => !cases.some(c => c.id === u.id) && !activeRequests.some(c => c.id === u.id))
                                                ]}
                                                showAssignButton={true}
                                                onAssign={handleBidOnCase}
                                                userType="lawyer"
                                                onSelectCase={setSelectedCase}
                                            />
                                        </div>
                                    </section>
                                </div>

                                {/* Right Column: Active Cases Sidebar */}
                                <div className="col-span-12 lg:col-span-5">
                                    <div className="bg-white dark:bg-surface-dark rounded-xl border border-slate-200/50 dark:border-slate-700/50 overflow-hidden shadow-sm sticky top-6">
                                        <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center">
                                            <div>
                                                <h2 className="font-bold text-slate-800 dark:text-slate-200 text-sm">Active Engagements</h2>
                                                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">Engagements where work has officially begun.</p>
                                            </div>
                                            <button
                                                onClick={fetchCases}
                                                disabled={casesLoading}
                                                className={`p-2 text-primary hover:bg-primary/10 rounded-lg transition-colors ${casesLoading ? 'animate-spin' : ''}`}
                                            >
                                                <span className="material-symbols-outlined text-xl">refresh</span>
                                            </button>
                                        </div>

                                        <div className="p-2">
                                            {cases.length === 0 ? (
                                                <div className="p-8 text-center text-slate-400 text-xs font-bold uppercase tracking-widest">
                                                    No Active Engagements
                                                </div>
                                            ) : (
                                                <div className="space-y-1">
                                                    {cases.map((caseItem) => (
                                                        <div
                                                            key={caseItem.id}
                                                            onClick={() => setSelectedCase(caseItem)}
                                                            className="grid grid-cols-12 items-center px-4 py-4 hover:bg-slate-50 dark:hover:bg-slate-800/50 rounded-lg group cursor-pointer transition-all"
                                                        >
                                                            <div className="col-span-2">
                                                                <span className="text-xs font-bold text-slate-800 dark:text-slate-200">#{caseItem.id}</span>
                                                            </div>
                                                            <div className="col-span-6">
                                                                <p className="text-xs font-black text-primary dark:text-white truncate pr-4">{caseItem.caseTitle}</p>
                                                                <p className="text-[10px] font-medium text-slate-400 mt-1 uppercase">
                                                                    Client: {caseItem.userFullName || 'Anonymous'} • {new Date(caseItem.updatedAt || caseItem.createdAt).toLocaleDateString()}
                                                                </p>
                                                            </div>
                                                            <div className="col-span-3">
                                                                <span className={`inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded border ${caseItem.caseStatus?.toLowerCase() === 'solved'
                                                                    ? 'text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-900/20 border-emerald-100 dark:border-emerald-900/50'
                                                                    : 'text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-900/20 border-amber-100 dark:border-amber-900/50'
                                                                    }`}>
                                                                    {caseItem.caseStatus?.replace('_', ' ') || 'ACTIVE'}
                                                                </span>
                                                            </div>
                                                            <div className="col-span-1 text-right flex items-center justify-end gap-2">
                                                                <span className="material-symbols-outlined text-slate-300 group-hover:text-primary transition-colors">chevron_right</span>
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>
                                            )}
                                        </div>

                                        <div className="p-4 border-t border-slate-100 dark:border-slate-800">
                                            <button
                                                className="w-full py-2.5 text-xs font-bold text-slate-500 dark:text-slate-400 hover:text-primary dark:hover:text-primary transition-colors uppercase tracking-widest"
                                            >
                                                View All Active Cases
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'profile' && (
                    <div className="profile-tab-content">
                        <LawyerProfile
                            lawyerId={lawyerId}
                            onUpdate={(updatedProfile) => {
                                setLawyerProfile(updatedProfile);
                            }}
                        />
                    </div>
                )}
            </div>

            {/* Help FAB */}
            <button className="fixed bottom-8 right-8 w-14 h-14 bg-slate-900 dark:bg-white text-white dark:text-slate-900 rounded-full shadow-2xl flex items-center justify-center hover:scale-110 active:scale-95 transition-all z-50">
                <span className="material-symbols-outlined">help</span>
            </button>
        </div>
    );
}

export default LawyerDashboard;
