import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Booking from './Booking';
import AppointmentsList from './AppointmentsList';
import CaseList from './CaseList';
import CaseDetail from './CaseDetail';
import AudioRecorder from './AudioRecorder';
import { casesApi } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import { toast } from 'react-toastify';
import CaseDraftPreview from './CaseDraftPreview';
import CaseCard from './CaseCard';

function UserDashboard() {
  const [activeTab, setActiveTab] = useState('dashboard'); // 'dashboard', 'cases', 'documents', 'appointments', 'settings'
  const { user, logout } = useAuth();
  const [cases, setCases] = useState([]);
  const [casesLoading, setCasesLoading] = useState(false);
  const [selectedCase, setSelectedCase] = useState(null);
  const [draftCaseId, setDraftCaseId] = useState(null); // For draft preview
  const [isRecorderOpen, setIsRecorderOpen] = useState(false);

  const navigate = useNavigate();
  const userId = user?.id ? parseInt(user.id) : null;

  // Safety return for unauthorized access
  if (!user || user.role !== 'user') return null;

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
    if (userId) {
      fetchCases();
    }
  }, [userId, fetchCases]);

  const handleLogout = () => {
    logout();
    navigate('/user-login');
  };

  const handleAudioUploadSuccess = async (data) => {
    // Fetch cases to get the newly created draft case
    await fetchCases();

    // Find the most recent draft case (should be the one just created)
    const response = await casesApi.getByUser(userId);
    const draftCase = response.data.find(c => c.caseStatus === 'DRAFT');

    if (draftCase) {
      setDraftCaseId(draftCase.id);
    }
  }

  const handlePublishSuccess = () => {
    setDraftCaseId(null);
    fetchCases();
  }

  const handleCancelDraft = () => {
    setDraftCaseId(null);
  }

  // Main content switching logic
  const renderMainContent = () => {
    // Show draft preview if there's a draft case
    if (draftCaseId) {
      return (
        <CaseDraftPreview
          caseId={draftCaseId}
          onPublish={handlePublishSuccess}
          onCancel={handleCancelDraft}
        />
      );
    }

    if (selectedCase) {
      return (
        <div className="p-8">
          <CaseDetail
            caseId={selectedCase.id}
            userType="user"
            userId={userId}
            onBack={() => { setSelectedCase(null); fetchCases(); }}
          />
        </div>
      );
    }

    switch (activeTab) {
      case 'dashboard':
        return (
          <div className="grid grid-cols-12 gap-8">
            {/* Center Column: Actions and Cases */}
            <div className="col-span-12 lg:col-span-8 flex flex-col gap-8">
              {/* Start New Case Card */}
              <section className="bg-primary rounded-2xl p-8 relative overflow-hidden shadow-xl shadow-primary/20 animate-in fade-in slide-in-from-bottom-4 duration-700">
                <div className="relative z-10 flex items-center justify-between">
                  <div className="max-w-md">
                    <h3 className="text-white text-2xl font-bold mb-2">Need Legal Assistance?</h3>
                    <p className="text-blue-100 text-base mb-6">Describe your situation using your voice. Our AI will analyze your intake and match you with the right lawyer in minutes.</p>
                    <button
                      onClick={() => setIsRecorderOpen(!isRecorderOpen)}
                      className="bg-white text-primary px-6 py-3 rounded-lg font-bold flex items-center gap-2 hover:bg-slate-100 transition-all shadow-lg group"
                    >
                      <span className="material-symbols-outlined group-hover:scale-110 transition-transform">
                        {isRecorderOpen ? 'close' : 'mic'}
                      </span>
                      {isRecorderOpen ? 'Close Recorder' : 'Record Your Situation'}
                    </button>
                  </div>
                  <div className="hidden md:flex items-center justify-center mr-10">
                    <div className="relative">
                      <div className="absolute inset-0 bg-white/20 rounded-full scale-125 animate-pulse"></div>
                      <div className="absolute inset-0 bg-white/10 rounded-full scale-150 animate-pulse" style={{ animationDelay: '0.5s' }}></div>
                      <div className="size-24 rounded-full bg-white flex items-center justify-center text-primary relative z-10 shadow-xl">
                        <span className="material-symbols-outlined text-4xl">mic</span>
                      </div>
                    </div>
                  </div>
                </div>
                {/* Audio Recorder Integration */}
                <div className={`mt-8 overflow-hidden transition-all duration-500 ${isRecorderOpen ? 'max-h-[500px] opacity-100' : 'max-h-0 opacity-0'}`}>
                  <div className="bg-white/5 backdrop-blur-md rounded-2xl p-6 border border-white/10">
                    <AudioRecorder userId={userId} onUploadSuccess={handleAudioUploadSuccess} />
                  </div>
                </div>
                {/* Decorative background element */}
                <div className="absolute -right-20 -bottom-20 size-80 bg-white/5 rounded-full"></div>
              </section>

              {/* Active Cases */}
              <section className="animate-in fade-in slide-in-from-bottom-6 duration-700 delay-100">
                <div className="flex justify-between items-end mb-6">
                  <h3 className="text-xl font-bold text-slate-900 dark:text-white">Active Intelligence Records</h3>
                  <button onClick={() => setActiveTab('cases')} className="text-primary dark:text-blue-400 text-sm font-semibold hover:underline">View All</button>
                </div>
                <div className="flex flex-col gap-4">
                  {cases.length === 0 ? (
                    <div className="p-12 text-center bg-white dark:bg-gray-900 border border-dashed border-slate-300 dark:border-slate-700 rounded-2xl group hover:border-primary/50 transition-colors cursor-pointer" onClick={() => setActiveTab('new-case')}>
                      <div className="size-16 mx-auto rounded-full bg-slate-50 dark:bg-slate-800 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                        <span className="material-symbols-outlined text-3xl text-slate-400 group-hover:text-primary transition-colors">add_circle</span>
                      </div>
                      <h4 className="text-slate-900 dark:text-white font-bold text-lg mb-1">No Active Records</h4>
                      <p className="text-slate-500 text-xs font-medium uppercase tracking-widest">Start a new legal intake</p>
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                      {cases.slice(0, 4).map((caseItem) => (
                        <CaseCard
                          key={caseItem.id}
                          caseItem={caseItem}
                          onClick={setSelectedCase}
                        />
                      ))}
                    </div>
                  )}
                </div>
              </section>
            </div>

            {/* Right Column: Sidebar Panels */}
            <div className="col-span-12 lg:col-span-4 flex flex-col gap-8">
              {/* Consultation Widget */}
              <section className="bg-white dark:bg-gray-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm animate-in fade-in slide-in-from-right-4 duration-700 delay-200">
                <div className="flex items-center gap-2 mb-6">
                  <span className="material-symbols-outlined text-primary text-xl">video_chat</span>
                  <h3 className="font-bold text-slate-900 dark:text-white uppercase tracking-tight">Upcoming Session</h3>
                </div>

                <AppointmentsList
                  userId={userId}
                  userType="user"
                  limit={1}
                  externalFilter="upcoming"
                />

                <button
                  onClick={() => setActiveTab('appointments')}
                  className="w-full mt-6 bg-primary text-white py-4 rounded-xl font-bold flex items-center justify-center gap-2 hover:opacity-95 transition-all shadow-lg shadow-primary/10 uppercase text-xs tracking-[0.2em]"
                >
                  <span className="material-symbols-outlined text-lg">calendar_month</span>
                  Manage All Bookings
                </button>
              </section>
            </div>
          </div>
        );
      case 'cases':
        return (
          <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
            <div className="flex justify-between items-center bg-white dark:bg-gray-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
              <div>
                <h2 className="text-2xl font-bold text-slate-900 dark:text-white tracking-tight">Legal Records</h2>
                <p className="text-slate-500 text-sm mt-1">A comprehensive repository of your digital justice matters.</p>
              </div>
              <button onClick={fetchCases} className="flex items-center gap-2 px-4 py-2 bg-slate-50 dark:bg-slate-800 text-slate-600 dark:text-slate-300 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-700 transition-all font-bold text-xs uppercase tracking-widest border border-slate-200 dark:border-slate-700">
                <span className="material-symbols-outlined text-lg">refresh</span>
                Sync Data
              </button>
            </div>
            {casesLoading ? (
              <div className="flex justify-center p-20 text-slate-400 font-bold uppercase tracking-[0.2em] text-xs animate-pulse">Establishing secure link...</div>
            ) : (
              <div className="bg-white dark:bg-gray-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
                <CaseList cases={cases} onSelectCase={setSelectedCase} userType="user" />
              </div>
            )}
          </div>
        );
      case 'appointments':
        return (
          <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
            <div className="flex justify-between items-center bg-white dark:bg-gray-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
              <div>
                <h2 className="text-2xl font-bold text-slate-900 dark:text-white tracking-tight">Consultation Sessions</h2>
                <p className="text-slate-500 text-sm mt-1">Manage your scheduled legal consultations and temporal links.</p>
              </div>
            </div>
            <div className="bg-white dark:bg-gray-900 p-8 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
              <AppointmentsList userId={userId} userType="user" />
            </div>
          </div>
        );
      default:
        return <div className="p-8 text-center text-slate-400 font-bold uppercase tracking-widest">Vault Under Development</div>;
    }
  };

  return (
    <div className="flex h-screen overflow-hidden bg-slate-50 dark:bg-slate-950 font-display selection:bg-primary/10">
      {/* Left Sidebar */}
      <aside className="w-64 flex-shrink-0 bg-white dark:bg-gray-900 border-r border-slate-200 dark:border-slate-800 flex flex-col justify-between py-8 transition-all duration-500 z-30">
        <div className="flex flex-col gap-10 px-6">
          {/* Logo */}
          <div className="flex items-center gap-3 group cursor-pointer">
            <div className="bg-primary size-11 rounded-2xl flex items-center justify-center text-white shadow-xl shadow-primary/20 group-hover:scale-110 transition-transform duration-500">
              <span className="material-symbols-outlined text-2xl">gavel</span>
            </div>
            <div>
              <h1 className="text-primary dark:text-white text-xl font-black leading-none tracking-tight">LegalConnect</h1>
              <p className="text-slate-500 text-[10px] font-black uppercase tracking-[0.2em] mt-1.5">Digital Justice</p>
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex flex-col gap-1.5">
            {[
              { id: 'dashboard', icon: 'dashboard', label: 'Dashboard' },
              { id: 'cases', icon: 'folder_open', label: 'My Cases' },
              { id: 'appointments', icon: 'calendar_today', label: 'Bookings' },
              { id: 'documents', icon: 'cloud_done', label: 'Vault' },
            ].map((item) => (
              <button
                key={item.id}
                onClick={() => { setActiveTab(item.id); setSelectedCase(null); }}
                className={`flex items-center gap-3.5 px-4 py-3.5 rounded-xl transition-all duration-300 group ${activeTab === item.id
                  ? 'bg-primary/5 text-primary dark:bg-primary dark:text-white font-bold shadow-sm'
                  : 'text-slate-500 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white'
                  }`}
              >
                <span className={`material-symbols-outlined text-[22px] transition-all duration-300 ${activeTab === item.id ? 'scale-110' : 'group-hover:scale-110'}`}>
                  {item.icon}
                </span>
                <span className="text-xs uppercase tracking-widest font-black">{item.label}</span>
              </button>
            ))}
          </nav>
        </div>

        {/* Profile Info & Logout */}
        <div className="px-6 border-t border-slate-100 dark:border-slate-800 pt-8 mt-auto space-y-6">
          <div className="flex items-center gap-3 p-3 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 transition-all hover:border-primary/20 cursor-pointer group/profile">
            <div
              className="size-10 rounded-full bg-slate-200 dark:bg-slate-700 bg-cover bg-center ring-2 ring-white dark:ring-slate-800 shadow-md group-hover/profile:ring-primary/20 transition-all"
              style={{ backgroundImage: "url('https://lh3.googleusercontent.com/aida-public/AB6AXuAi9iPzPtwuZ4Atb5jjHCA3GBO65fslUiWmg5rvZRf0mEb95sHxJPffkH5LNx1L5tEPgLcUBAxrA6zREdwRQ9vmgbznxNQoyXu2FszYFvxSFWChuqpdrh4TsIqGCX4BrSdv6zncRC4XfssSMOxseHpKnndemNxatYjA6syYz6B6FApH2C1zKo-bWpd_VYIV9xyzKuuVB7RZV2k8jKqskzH3zqWBDxKWncM829vnKp3Lc0YrqZYU-4crdZHMEUfcB9WrrP1TA4MYXrA')" }}
            ></div>
            <div className="overflow-hidden">
              <p className="text-sm font-bold text-slate-900 dark:text-white truncate uppercase tracking-tight">{user.fullName || user.username}</p>
              <p className="text-[10px] text-slate-500 font-black uppercase tracking-widest truncate">Premium Entity</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 px-4 py-3.5 rounded-xl bg-rose-500/5 hover:bg-rose-500 text-rose-500 hover:text-white transition-all duration-300 font-bold text-xs uppercase tracking-widest border border-rose-500/10 group"
          >
            <span className="material-symbols-outlined text-lg group-hover:-translate-x-1 transition-transform">logout</span>
            Logout
          </button>
        </div>
      </aside>

      {/* Main Content Scroll Area */}
      <main className="flex-1 overflow-y-auto bg-slate-50 dark:bg-slate-950 p-10 scroll-smooth">
        {/* Top Header */}
        <header className="flex justify-between items-center mb-10 pb-6 border-b border-slate-200/50 dark:border-slate-800/50">
          <div>
            <h2 className="text-4xl font-black text-slate-950 dark:text-white tracking-tight leading-tight">
              Hello, {user.fullName?.split(' ')[0] || user.username}
            </h2>
            <p className="text-slate-500 dark:text-slate-400 mt-2 font-medium">Welcome back. Monitoring your active legal intelligence matters.</p>
          </div>
          <div className="flex items-center gap-6">

            <div className="text-right hidden sm:block">
              <p className="text-xs font-black text-slate-900 dark:text-white uppercase tracking-widest">
                {new Date().toLocaleDateString('en-US', { weekday: 'long' })}
              </p>
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1">
                {new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
              </p>
            </div>
          </div>
        </header>

        {renderMainContent()}
      </main>

    </div>
  );
}

export default UserDashboard;

