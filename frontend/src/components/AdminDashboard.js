import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminApi } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import { toast } from 'react-toastify';
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, LineElement, PointElement, ArcElement, Title, Tooltip, Legend } from 'chart.js';
import { Bar, Line, Pie } from 'react-chartjs-2';

// Register Chart.js components
ChartJS.register(CategoryScale, LinearScale, BarElement, LineElement, PointElement, ArcElement, Title, Tooltip, Legend);

const AdminDashboard = () => {
    const [activeTab, setActiveTab] = useState('dashboard');
    const [managementTab, setManagementTab] = useState('users');
    const [stats, setStats] = useState({ totalUsers: 0, totalLawyers: 0, totalCases: 0, pendingAudits: 0 });
    const [users, setUsers] = useState([]);
    const [lawyers, setLawyers] = useState([]);
    const [cases, setCases] = useState([]);
    const [appointments, setAppointments] = useState([]);
    const [auditLogs, setAuditLogs] = useState([]);
    const [analytics, setAnalytics] = useState(null);
    const [settings, setSettings] = useState([]);
    const [loading, setLoading] = useState(false);
    const [statsLoading, setStatsLoading] = useState(false);
    const [settingsLoading, setSettingsLoading] = useState(false);
    const [selectedItem, setSelectedItem] = useState(null);
    const [showModal, setShowModal] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [currentPage, setCurrentPage] = useState(0);

    const { user, logout, loading: authLoading } = useAuth();
    const navigate = useNavigate();

    // Fetch dashboard stats
    useEffect(() => {
        if (user && user.role === 'admin') {
            fetchStats();
        }
    }, [user]);

    // Fetch data based on management tab
    useEffect(() => {
        if (!user || user.role !== 'admin') return;

        if (managementTab === 'users') fetchUsers(currentPage);
        else if (managementTab === 'lawyers') fetchLawyers(currentPage);
        else if (managementTab === 'cases') fetchCases(currentPage);
        else if (managementTab === 'appointments') fetchAppointments();
    }, [managementTab, user, currentPage]);

    // Reset pagination when switching tabs
    useEffect(() => {
        setCurrentPage(0);
    }, [managementTab, activeTab]);

    // Fetch data when switching to Analytics or Audit tabs
    useEffect(() => {
        if (!user || user.role !== 'admin') return;

        if (activeTab === 'analytics' && !analytics) {
            fetchAnalytics();
        } else if (activeTab === 'audit') {
            fetchAuditLogs(currentPage);
        } else if (activeTab === 'settings' && settings.length === 0) {
            fetchSettings();
        }
    }, [activeTab, user, currentPage]);

    const fetchStats = async () => {
        setStatsLoading(true);
        try {
            const response = await adminApi.getStats();
            setStats(response.data);
        } catch (error) {
            console.error('Error fetching stats:', error);
            handleError(error);
        } finally {
            setStatsLoading(false);
        }
    };

    const fetchUsers = async (page = 0) => {
        setLoading(true);
        try {
            const response = await adminApi.getUsers(page);
            setUsers(response.data.content || []);
        } catch (error) {
            console.error('Error fetching users:', error);
            handleError(error);
        } finally {
            setLoading(false);
        }
    };

    const fetchLawyers = async (page = 0) => {
        setLoading(true);
        try {
            const response = await adminApi.getLawyers(page);
            setLawyers(response.data.content || []);
        } catch (error) {
            console.error('Error fetching lawyers:', error);
            handleError(error);
        } finally {
            setLoading(false);
        }
    };

    const fetchCases = async (page = 0) => {
        setLoading(true);
        try {
            const response = await adminApi.getCases(page);
            setCases(response.data.content || []);
        } catch (error) {
            console.error('Error fetching cases:', error);
            handleError(error);
        } finally {
            setLoading(false);
        }
    };

    const fetchAppointments = async () => {
        setLoading(true);
        try {
            const response = await adminApi.getAppointments(user.id);
            setAppointments(Array.isArray(response.data) ? response.data : []);
        } catch (error) {
            console.error('Error fetching appointments:', error);
            handleError(error);
        } finally {
            setLoading(false);
        }
    };

    const fetchAnalytics = async () => {
        setLoading(true);
        try {
            const response = await adminApi.getAnalytics();
            setAnalytics(response.data);
        } catch (error) {
            console.error('Error fetching analytics:', error);
            handleError(error);
        } finally {
            setLoading(false);
        }
    };

    const fetchAuditLogs = async (page = 0) => {
        setLoading(true);
        try {
            const response = await adminApi.getAuditLogs(page, 20);
            setAuditLogs(response.data.content || []);
        } catch (error) {
            console.error('Error fetching audit logs:', error);
            handleError(error);
        } finally {
            setLoading(false);
        }
    };

    const fetchSettings = async () => {
        setSettingsLoading(true);
        try {
            const response = await adminApi.getSettings();
            setSettings(response.data || []);
        } catch (error) {
            console.error('Error fetching settings:', error);
            handleError(error);
        } finally {
            setSettingsLoading(false);
        }
    };

    const handleUpdateSetting = async (key, value) => {
        try {
            await adminApi.updateSetting({ settingKey: key, settingValue: String(value) });
            toast.success(`Protocol ${key} updated`);
            fetchSettings();
        } catch (error) {
            handleError(error);
        }
    };

    const handleDelete = async (type, id) => {
        if (!window.confirm(`Terminate ${type} entry permanently?`)) return;

        try {
            if (type === 'users') await adminApi.deleteUser(id);
            else if (type === 'lawyers') await adminApi.deleteLawyer(id);
            else if (type === 'cases') await adminApi.deleteCase(id);
            else if (type === 'appointments') await adminApi.deleteAppointment(id, user.id);

            toast.success(`Entry purged from system`);

            if (type === 'users') fetchUsers();
            else if (type === 'lawyers') fetchLawyers();
            else if (type === 'cases') fetchCases();
            else if (type === 'appointments') fetchAppointments();
            fetchStats();
        } catch (error) {
            console.error(`Error deleting ${type}:`, error);
            toast.error(`Failed to purge ${type}`);
        }
    };

    const handleEdit = (item, type) => {
        setSelectedItem({ ...item, type });
        setEditMode(true);
        setShowModal(true);
    };

    const handleView = (item, type) => {
        setSelectedItem({ ...item, type });
        setEditMode(false);
        setShowModal(true);
    };

    const handleSave = async () => {
        const { type, id, ...updateData } = selectedItem;

        try {
            if (type === 'users') await adminApi.updateUser(id, updateData);
            else if (type === 'lawyers') await adminApi.updateLawyer(id, updateData);
            else if (type === 'cases') await adminApi.updateCase(id, updateData);

            toast.success(`${type} updated successfully`);
            setShowModal(false);

            if (type === 'users') fetchUsers();
            else if (type === 'lawyers') fetchLawyers();
            else if (type === 'cases') fetchCases();
            fetchStats();
        } catch (error) {
            console.error('Error updating:', error);
            toast.error('Failed to update');
        }
    };

    const handleReassignCase = async (caseId) => {
        const lawyerId = prompt('Enter new Lawyer ID:');
        if (!lawyerId) return;

        try {
            await adminApi.reassignCase(caseId, parseInt(lawyerId));
            toast.success('Case reassigned successfully');
            fetchCases();
            fetchStats();
        } catch (error) {
            console.error('Error reassigning case:', error);
            toast.error('Failed to reassign case');
        }
    };

    const handleVerifyCase = async (caseId) => {
        try {
            await adminApi.updateCase(caseId, { caseStatus: 'VERIFIED' });
            toast.success('Protocol verified successfully');
            fetchCases();
            fetchStats();
        } catch (error) {
            console.error('Error verifying case:', error);
            toast.error('Failed to verify case');
        }
    };

    const handleVerifyLawyer = async (lawyerId) => {
        try {
            await adminApi.verifyLawyer(lawyerId, true);
            toast.success('Counsel credentials verified');
            fetchLawyers();
            fetchStats();
        } catch (error) {
            console.error('Error verifying lawyer:', error);
            toast.error('Failed to verify counsel');
        }
    };

    const handleError = (error) => {
        if (error.response?.status === 401 || error.response?.status === 403) {
            logout();
            navigate('/admin/login');
        } else {
            toast.error('System synchronization error');
        }
    };

    if (authLoading) {
        return (
            <div className="min-h-screen bg-slate-950 flex items-center justify-center text-white font-black uppercase tracking-widest animate-pulse">
                Establishing Secure Session...
            </div>
        );
    }

    if (!user || user.role !== 'admin') {
        return null; // ProtectedRoute handles redirect
    }

    const navItems = [
        { id: 'dashboard', label: 'Dashboard', icon: 'dashboard' },
        { id: 'analytics', label: 'Analytics', icon: 'analytics' },
        { id: 'audit', label: 'Audit Logs', icon: 'verified_user' },
        { id: 'settings', label: 'System Settings', icon: 'settings' }
    ];

    return (
        <div className="flex min-h-screen bg-slate-50 dark:bg-slate-950 font-display">
            {/* Premium Sidebar */}
            <aside className="fixed left-0 top-0 h-screen w-72 glass-morphism dark:bg-[#0f172a]/80 backdrop-blur-2xl border-r border-white/10 z-50 flex flex-col shadow-2xl">
                <div className="p-10">
                    <div className="flex items-center gap-3 group px-2">
                        <div className="w-9 h-9 bg-[#1a405b] rounded-xl flex items-center justify-center text-white shadow-lg shadow-primary/20 hover:rotate-12 transition-transform duration-500">
                            <span className="material-symbols-outlined text-xl">gavel</span>
                        </div>
                        <div className="flex flex-col">
                            <span className="text-lg font-bold tracking-tighter text-slate-900 dark:text-white">LegalConnect</span>
                            <span className="text-[9px] font-black text-slate-400 uppercase tracking-[0.2em] -mt-1">Justice Platform</span>
                        </div>
                    </div>
                </div>

                <nav className="flex-1 px-6 space-y-2 mt-4">
                    {navItems.map((item) => (
                        <button
                            key={item.id}
                            onClick={() => setActiveTab(item.id)}
                            className={`w-full flex items-center gap-4 px-6 py-4 rounded-2xl text-[11px] font-black uppercase tracking-[0.15em] transition-all duration-500 group relative overflow-hidden ${activeTab === item.id
                                ? 'text-[#1a405b] dark:text-cyber-blue shadow-lg shadow-primary/5 dark:shadow-cyber-blue/5'
                                : 'text-slate-400 hover:text-slate-900 dark:hover:text-white'
                                }`}
                        >
                            {activeTab === item.id && (
                                <div className="absolute left-0 w-1.5 h-6 bg-[#1a405b] dark:bg-cyber-blue rounded-r-full animate-pulse"></div>
                            )}
                            <span className={`material-symbols-outlined transition-all duration-500 ${activeTab === item.id ? 'scale-110' : 'group-hover:translate-x-1'}`}>
                                {item.icon}
                            </span>
                            {item.label}
                        </button>
                    ))}
                </nav>

                <div className="p-8 mt-auto">
                    <div className="p-6 rounded-[2rem] bg-slate-900/5 dark:bg-white/5 border border-white/5 relative overflow-hidden group">
                        <div className="absolute -right-4 -top-4 w-20 h-20 bg-[#1a405b]/10 rounded-full blur-2xl group-hover:bg-primary/20 transition-all duration-700"></div>
                        <div className="flex items-center gap-4 relative z-10">
                            <div className="w-12 h-12 rounded-2xl bg-white dark:bg-slate-800 bg-cover bg-center border border-white/10 shadow-lg group-hover:rotate-6 transition-transform duration-500"
                                style={{ backgroundImage: `url('https://ui-avatars.com/api/?name=${user.fullName || user.username}&background=0f172a&color=fff')` }}></div>
                            <div className="overflow-hidden">
                                <p className="text-[11px] font-black uppercase truncate text-slate-900 dark:text-white tracking-tight leading-none mb-1">{user.fullName || user.username}</p>
                                <p className="text-[9px] text-slate-400 font-black uppercase tracking-widest italic opacity-60">System Admin</p>
                            </div>
                        </div>
                        <button onClick={logout} className="mt-4 w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-white dark:bg-slate-800 text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-500/10 transition-all group/btn border border-white/10">
                            <span className="material-symbols-outlined text-sm font-black group-hover/btn:rotate-180 transition-transform">logout</span>
                            <span className="text-[10px] font-black uppercase tracking-widest">Logout</span>
                        </button>
                    </div>
                </div>
            </aside>

            {/* Main Content */}
            <main className="flex-1 ml-72 p-10 min-h-screen bg-slate-50/50 dark:bg-[#080c14] transition-colors duration-700">
                <header className="flex justify-between items-end mb-16 px-4">
                    <div className="space-y-2">
                        <div className="flex items-center gap-4">
                            <div className="px-3 py-1 bg-[#1a405b]/10 dark:bg-cyber-blue/10 border border-primary/20 dark:border-cyber-blue/20 rounded-full">
                                <span className="text-[9px] font-black text-[#1a405b] dark:text-cyber-blue uppercase tracking-widest animate-pulse">Live Status</span>
                            </div>
                            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-[0.3em]">Admin Portal v4.2</span>
                        </div>
                        <h1 className="text-5xl font-black text-slate-900 dark:text-white tracking-tighter filter drop-shadow-sm">Admin Dashboard</h1>
                        <p className="text-slate-500 font-bold uppercase text-[10px] tracking-[0.2em] opacity-60">Administrator Access Only • {new Date().toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}</p>
                    </div>
                    <div className="flex items-center gap-6">
                        <div className="hidden lg:flex flex-col items-end mr-2">
                            <p className="text-[10px] text-slate-400 font-black uppercase tracking-[0.2em]">Security Status</p>
                            <p className="text-xs font-black text-emerald-500 uppercase tracking-widest">Level Alpha 1</p>
                        </div>
                        <button className="w-14 h-14 bg-white dark:bg-slate-900 rounded-[1.25rem] shadow-xl text-slate-400 border border-slate-100 dark:border-slate-800 hover:text-[#1a405b] dark:hover:text-cyber-blue hover:scale-110 hover:rotate-6 transition-all duration-500 flex items-center justify-center relative">
                            <span className="material-symbols-outlined scale-110">notifications</span>
                            <div className="absolute top-4 right-4 w-2 h-2 bg-rose-500 rounded-full animate-ping"></div>
                        </button>
                        <button className="flex items-center gap-3 bg-slate-900 dark:bg-white text-white dark:text-slate-900 px-8 py-4 rounded-[1.5rem] text-[10px] font-black uppercase tracking-[0.2em] shadow-2xl hover:scale-105 active:scale-95 transition-all duration-500 group overflow-hidden relative">
                            <div className="absolute inset-0 bg-[#1a405b] opacity-0 group-hover:opacity-10 transition-opacity"></div>
                            <span className="material-symbols-outlined text-lg group-hover:rotate-90 transition-transform duration-500">add_circle</span>
                            Add New Record
                        </button>
                    </div>
                </header>

                {/* Metrics */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-10 mb-16 px-2">
                    {[
                        { label: 'Total Users', val: stats.totalUsers || 0, icon: 'diversity_3', color: 'blue', trend: '+12.4%', detail: 'Total Network' },
                        { label: 'Total Lawyers', val: stats.totalLawyers || 0, icon: 'cognition', color: 'cyan', trend: '+8.1%', detail: 'Verified Profiles' },
                        { label: 'Total Cases', val: stats.totalCases || 0, icon: 'folder_data', color: 'indigo', trend: 'Synced', detail: 'Cloud Storage' },
                        { label: 'Pending Verification', val: stats.pendingAudits || 0, icon: 'security_update_warning', color: 'rose', trend: 'CRITICAL', detail: 'Immediate Review' }
                    ].map((card, i) => (
                        <div key={i} className="group relative">
                            <div className="absolute -inset-0.5 bg-gradient-to-r from-[#1a405b]/20 to-cyber-blue/20 rounded-[3rem] blur opacity-0 group-hover:opacity-100 transition duration-1000 group-hover:duration-200"></div>
                            <div className="relative bg-white dark:bg-slate-900/40 p-7 rounded-[2rem] shadow-xl border border-slate-100 dark:border-white/5 flex flex-col justify-between h-48 transition-all duration-500 group-hover:translate-y-[-10px] backdrop-blur-xl">
                                <div className="flex justify-between items-start">
                                    <div className={`w-12 h-12 rounded-2xl flex items-center justify-center transition-all duration-700 group-hover:rotate-[360deg] ${card.color === 'rose' ? 'bg-rose-500/10 text-rose-500 shadow-lg shadow-rose-500/20' : 'bg-[#1a405b]/10 dark:bg-cyber-blue/10 text-[#1a405b] dark:text-cyber-blue shadow-lg shadow-primary/10 dark:shadow-cyber-blue/20'}`}>
                                        <span className="material-symbols-outlined text-xl font-black">{card.icon}</span>
                                    </div>
                                    <div className={`px-2 py-0.5 rounded-full text-[8px] font-black uppercase tracking-widest ${card.color === 'rose' ? 'bg-rose-500 text-white' : 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20'}`}>
                                        {card.trend}
                                    </div>
                                </div>
                                <div className="mt-4">
                                    <h3 className={`text-4xl font-black tracking-tighter text-slate-900 dark:text-white group-hover:scale-105 transition-transform origin-left`}>{card.val}</h3>
                                    <div className="flex flex-col mt-2">
                                        <p className="text-[10px] font-black text-slate-400 dark:text-slate-500 uppercase tracking-[0.2em] flex items-center gap-2">
                                            <div className={`w-1 h-1 rounded-full ${card.color === 'rose' ? 'bg-rose-500 animate-pulse' : 'bg-emerald-500'}`}></div>
                                            {card.label}
                                        </p>
                                        <p className="text-[9px] text-slate-300 dark:text-slate-600 font-bold uppercase tracking-[0.1em] mt-0.5">{card.detail}</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Analytics Tab */}
                {activeTab === 'analytics' && analytics && (
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                        {/* User Growth Chart */}
                        <div className="bg-white dark:bg-slate-900 p-8 rounded-[3rem] shadow-xl shadow-slate-200/50 dark:shadow-none border border-slate-100 dark:border-slate-800">
                            <h3 className="text-xl font-black text-slate-900 dark:text-white mb-6">User Growth Trend</h3>
                            <Line
                                data={{
                                    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
                                    datasets: [{
                                        label: 'Total Users',
                                        data: analytics.userGrowth,
                                        borderColor: '#1a405b',
                                        backgroundColor: 'rgba(26, 64, 91, 0.1)',
                                        tension: 0.4
                                    }]
                                }}
                                options={{ responsive: true, maintainAspectRatio: true }}
                            />
                        </div>

                        {/* Case Distribution Chart */}
                        <div className="bg-white dark:bg-slate-900 p-8 rounded-[3rem] shadow-xl shadow-slate-200/50 dark:shadow-none border border-slate-100 dark:border-slate-800">
                            <h3 className="text-xl font-black text-slate-900 dark:text-white mb-6">Case Type Distribution</h3>
                            <Pie
                                data={{
                                    labels: Object.keys(analytics.caseDistribution),
                                    datasets: [{
                                        data: Object.values(analytics.caseDistribution),
                                        backgroundColor: ['#1a405b', '#2563eb', '#7c3aed', '#db2777', '#f59e0b']
                                    }]
                                }}
                                options={{ responsive: true, maintainAspectRatio: true }}
                            />
                        </div>

                        {/* Case Status Breakdown */}
                        <div className="bg-white dark:bg-slate-900 p-8 rounded-[3rem] shadow-xl shadow-slate-200/50 dark:shadow-none border border-slate-100 dark:border-slate-800 lg:col-span-2">
                            <h3 className="text-xl font-black text-slate-900 dark:text-white mb-6">Case Status Breakdown</h3>
                            <Bar
                                data={{
                                    labels: Object.keys(analytics.caseStatus),
                                    datasets: [{
                                        label: 'Cases by Status',
                                        data: Object.values(analytics.caseStatus),
                                        backgroundColor: '#1a405b'
                                    }]
                                }}
                                options={{ responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } }}
                                height={80}
                            />
                        </div>
                    </div>
                )}

                {/* Audit Logs Tab (Premium Design) */}
                {activeTab === 'audit' && (
                    <div className="space-y-8">
                        <div className="flex justify-between items-center mb-10 px-4">
                            <div>
                                <h3 className="text-3xl font-black text-slate-900 dark:text-white tracking-tighter">System Activity Log</h3>
                                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-[0.3em] mt-2">Log of recent system actions</p>
                            </div>
                            <button onClick={() => fetchAuditLogs(currentPage)} className="w-12 h-12 glass-morphism dark:bg-white/5 rounded-2xl flex items-center justify-center text-slate-400 hover:text-primary dark:hover:text-cyber-blue transition-all border border-white/5 shadow-xl">
                                <span className="material-symbols-outlined">refresh</span>
                            </button>
                        </div>

                        <div className="grid grid-cols-1 gap-4">
                            {loading ? (
                                Array(5).fill(0).map((_, i) => (
                                    <div key={i} className="h-24 rounded-[1.5rem] bg-slate-100 dark:bg-slate-900 animate-pulse border border-slate-200 dark:border-white/5"></div>
                                ))
                            ) : auditLogs.length === 0 ? (
                                <div className="py-20 text-center glass-morphism dark:bg-white/5 rounded-[2.5rem] border border-white/5 shadow-2xl">
                                    <span className="material-symbols-outlined text-6xl text-slate-200 dark:text-slate-800 mb-4">history_toggle_off</span>
                                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">No activity logs found for this period</p>
                                </div>
                            ) : (
                                auditLogs.map((log, i) => (
                                    <div key={i} className="group relative">
                                        <div className="absolute -inset-0.5 bg-gradient-to-r from-emerald-500/10 to-transparent rounded-[1.5rem] blur opacity-0 group-hover:opacity-100 transition duration-500"></div>
                                        <div className="relative flex items-center gap-6 p-6 bg-white dark:bg-slate-900/60 rounded-[1.5rem] border border-slate-100 dark:border-white/5 shadow-premium hover:translate-x-2 transition-all duration-300 backdrop-blur-md">
                                            <div className={`w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:scale-110 transition-transform ${log.action === 'DELETE' ? 'bg-rose-500/10 text-rose-500' :
                                                log.action === 'CREATE' ? 'bg-emerald-500/10 text-emerald-500' :
                                                    'bg-[#1a405b]/10 dark:bg-cyber-blue/10 text-[#1a405b] dark:text-cyber-blue'
                                                }`}>
                                                <span className="material-symbols-outlined text-xl font-black">
                                                    {log.action === 'DELETE' ? 'delete_forever' : log.action === 'CREATE' ? 'add_box' : log.action === 'VERIFY' ? 'verified' : 'published_with_changes'}
                                                </span>
                                            </div>
                                            <div className="flex-1 overflow-hidden">
                                                <div className="flex items-center justify-between mb-1">
                                                    <p className="text-[11px] font-black text-slate-900 dark:text-white uppercase tracking-tight">
                                                        {log.adminName} <span className="text-primary dark:text-cyber-blue">{log.action.toLowerCase()}</span> {log.targetType.toLowerCase()} #{log.targetId}
                                                    </p>
                                                    <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest bg-slate-50 dark:bg-white/5 px-3 py-1 rounded-full">
                                                        {new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                                                    </span>
                                                </div>
                                                <div className="flex items-center gap-4">
                                                    <p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest flex items-center gap-1">
                                                        <span className="material-symbols-outlined text-[10px]">person</span> {log.adminUsername || log.adminName || 'System'}
                                                    </p>
                                                    <div className="w-1 h-1 bg-slate-200 dark:bg-slate-700 rounded-full"></div>
                                                    <p className="text-[9px] font-bold text-slate-400 uppercase tracking-widest truncate">{log.details || `ENTITY: ${log.entityType || log.targetType}`}</p>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>

                        {/* Pagination - Minimalist */}
                        <div className="flex justify-center gap-2 py-8">
                            <button onClick={() => setCurrentPage(p => Math.max(0, p - 1))} className="px-6 py-2 glass-morphism dark:bg-white/5 border border-white/5 rounded-xl text-[10px] font-black uppercase tracking-widest text-slate-400 hover:text-white transition-all shadow-xl disabled:opacity-30" disabled={currentPage === 0}>Previous</button>
                            <button onClick={() => setCurrentPage(p => p + 1)} className="px-6 py-2 glass-morphism dark:bg-white/5 border border-white/5 rounded-xl text-[10px] font-black uppercase tracking-widest text-slate-400 hover:text-white transition-all shadow-xl">Next</button>
                        </div>
                    </div>
                )}

                {/* System Settings Tab (High-Tech Interface) */}
                {activeTab === 'settings' && (
                    <div className="space-y-10 animate-in fade-in slide-in-from-bottom-5 duration-700">
                        <div className="flex justify-between items-end mb-10 px-4">
                            <div>
                                <h3 className="text-3xl font-black text-slate-900 dark:text-white tracking-tighter">System Settings</h3>
                                <p className="text-[10px] text-slate-400 font-bold uppercase tracking-[0.3em] mt-2">Adjust system configurations and AI parameters</p>
                            </div>
                            <div className="px-6 py-2 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
                                <span className="text-[9px] font-black text-emerald-500 uppercase tracking-widest">Settings Up to Date</span>
                            </div>
                        </div>

                        <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
                            {['SYSTEM', 'AI', 'SECURITY'].map(category => (
                                <div key={category} className="bg-white dark:bg-slate-900/60 rounded-[2.5rem] border border-slate-100 dark:border-white/5 shadow-2xl backdrop-blur-xl p-10">
                                    <div className="flex items-center gap-3 mb-10">
                                        <div className="w-10 h-10 rounded-xl bg-[#1a405b]/10 dark:bg-cyber-blue/10 flex items-center justify-center text-primary dark:text-cyber-blue">
                                            <span className="material-symbols-outlined text-xl">
                                                {category === 'SYSTEM' ? 'settings_suggest' : category === 'AI' ? 'psychology' : 'admin_panel_settings'}
                                            </span>
                                        </div>
                                        <h4 className="text-xs font-black uppercase tracking-[0.4em] text-slate-400">{category} SETTINGS</h4>
                                    </div>

                                    <div className="space-y-6">
                                        {settingsLoading ? (
                                            Array(3).fill(0).map((_, i) => (
                                                <div key={i} className="h-16 rounded-2xl bg-slate-100 dark:bg-slate-800 animate-pulse"></div>
                                            ))
                                        ) : settings.filter(s => s.category === category).length === 0 ? (
                                            <p className="text-[10px] font-black text-slate-500 uppercase tracking-widest text-center py-10 opacity-40">No settings found</p>
                                        ) : settings.filter(s => s.category === category).map((setting) => (
                                            <div key={setting.id} className="group p-5 bg-slate-50 dark:bg-white/5 rounded-[1.5rem] border border-transparent hover:border-white/10 transition-all flex items-center justify-between">
                                                <div>
                                                    <p className="text-[10px] font-black text-slate-900 dark:text-white uppercase tracking-widest mb-1">{setting.settingKey.replace(/_/g, ' ')}</p>
                                                    <p className="text-[9px] text-slate-400 font-bold uppercase tracking-tight opacity-60">{setting.description}</p>
                                                </div>

                                                {setting.settingValue === 'true' || setting.settingValue === 'false' ? (
                                                    <button
                                                        onClick={() => handleUpdateSetting(setting.settingKey, setting.settingValue === 'true' ? 'false' : 'true')}
                                                        className={`w-14 h-8 rounded-full relative transition-all duration-500 ${setting.settingValue === 'true' ? 'bg-emerald-500 shadow-lg shadow-emerald-500/30' : 'bg-slate-300 dark:bg-slate-700'}`}
                                                    >
                                                        <div className={`absolute top-1 w-6 h-6 bg-white rounded-full transition-all duration-500 shadow-md ${setting.settingValue === 'true' ? 'left-7' : 'left-1'}`}></div>
                                                    </button>
                                                ) : (
                                                    <div className="flex items-center gap-2">
                                                        <input
                                                            className="bg-white dark:bg-slate-950 border border-slate-100 dark:border-white/5 rounded-xl px-4 py-2 text-[10px] font-black uppercase tracking-widest w-40 text-primary dark:text-cyber-blue outline-none focus:ring-2 focus:ring-primary/20"
                                                            value={setting.settingValue}
                                                            onChange={(e) => {
                                                                const newSettings = [...settings];
                                                                const idx = newSettings.findIndex(s => s.id === setting.id);
                                                                newSettings[idx].settingValue = e.target.value;
                                                                setSettings(newSettings);
                                                            }}
                                                            onBlur={() => handleUpdateSetting(setting.settingKey, setting.settingValue)}
                                                        />
                                                    </div>
                                                )}
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            ))}

                            <div className="bg-[#1a405b] dark:bg-cyber-blue/10 rounded-[2.5rem] p-12 overflow-hidden relative group">
                                <div className="absolute top-0 right-0 -translate-y-1/2 translate-x-1/4 w-80 h-80 bg-white/5 rounded-full blur-3xl group-hover:scale-150 transition-transform duration-1000"></div>
                                <h2 className="text-4xl font-black text-white dark:text-cyber-blue tracking-tighter mb-4 relative z-10 leading-tight">Manual Settings Override.</h2>
                                <p className="text-white/60 text-xs font-black uppercase tracking-[0.2em] relative z-10">Manual adjustment of core system settings</p>
                                <button className="mt-12 px-10 py-5 bg-white dark:bg-cyber-blue text-slate-900 dark:text-slate-900 rounded-[1.5rem] text-[10px] font-black uppercase tracking-[0.3em] shadow-2xl relative z-10 hover:scale-105 active:scale-95 transition-all">
                                    Apply Manual Override
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {/* Management Section (Premium Card Grid) */}
                {activeTab === 'dashboard' && (
                    <div className="space-y-10">
                        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-8 bg-white dark:bg-slate-900/40 p-8 rounded-[2.5rem] border border-slate-100 dark:border-white/5 backdrop-blur-xl shadow-2xl">
                            <div className="flex p-2 bg-slate-100 dark:bg-slate-800 rounded-[1.8rem] w-fit shadow-inner">
                                {[
                                    { id: 'users', label: 'Users', icon: 'person' },
                                    { id: 'lawyers', label: 'Lawyers', icon: 'balance' },
                                    { id: 'cases', label: 'Cases', icon: 'folder' },
                                    { id: 'appointments', label: 'Appointments', icon: 'event' }
                                ].map(tab => (
                                    <button
                                        key={tab.id}
                                        onClick={() => setManagementTab(tab.id)}
                                        className={`px-8 py-3.5 text-[10px] font-black uppercase tracking-widest rounded-[1.5rem] transition-all flex items-center gap-3 overflow-hidden group/tab relative ${managementTab === tab.id
                                            ? 'bg-slate-900 dark:bg-white text-white dark:text-slate-900 shadow-xl scale-105'
                                            : 'text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                                            }`}
                                    >
                                        <span className={`material-symbols-outlined text-sm transition-transform duration-500 ${managementTab === tab.id ? 'rotate-[360deg]' : 'group-hover/tab:rotate-12'}`}>{tab.icon}</span>
                                        {tab.label}
                                    </button>
                                ))}
                            </div>
                            <div className="relative group flex-1 max-w-md">
                                <div className="absolute inset-y-0 left-5 flex items-center pointer-events-none">
                                    <span className="material-symbols-outlined text-slate-400 transition-colors group-focus-within:text-[#1a405b] group-focus-within:dark:text-cyber-blue">search</span>
                                </div>
                                <input
                                    className="pl-14 pr-6 py-5 w-full bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/5 rounded-[1.5rem] text-[10px] font-black uppercase tracking-[0.2em] focus:ring-4 focus:ring-primary/5 outline-none transition-all placeholder:text-slate-400 placeholder:opacity-50"
                                    placeholder={`Search ${managementTab}...`}
                                    type="text"
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                />
                            </div>
                        </div>

                        <div className="bg-white dark:bg-slate-900/60 rounded-[2.5rem] border border-slate-100 dark:border-white/5 shadow-2xl backdrop-blur-xl overflow-hidden">
                            <div className="overflow-x-auto">
                                <table className="w-full text-left border-collapse">
                                    <thead>
                                        <tr className="border-b border-slate-100 dark:border-white/5 bg-slate-50/50 dark:bg-white/5">
                                            <th className="px-8 py-6 text-[10px] font-black text-slate-400 uppercase tracking-[0.3em]">User Info</th>
                                            <th className="px-8 py-6 text-[10px] font-black text-slate-400 uppercase tracking-[0.3em]">Status</th>
                                            <th className="px-8 py-6 text-[10px] font-black text-slate-400 uppercase tracking-[0.3em]">Date Created</th>
                                            <th className="px-8 py-6 text-[10px] font-black text-slate-400 uppercase tracking-[0.3em] text-right">Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-50 dark:divide-white/5">
                                        {loading ? (
                                            Array(6).fill(0).map((_, i) => (
                                                <tr key={i} className="animate-pulse">
                                                    <td colSpan="4" className="px-8 py-6">
                                                        <div className="h-10 bg-slate-100 dark:bg-slate-800 rounded-xl w-full"></div>
                                                    </td>
                                                </tr>
                                            ))
                                        ) : (managementTab === 'users' ? users : managementTab === 'lawyers' ? lawyers : managementTab === 'cases' ? cases : appointments)
                                            .filter(item => {
                                                const search = searchTerm.toLowerCase();
                                                if (managementTab === 'cases') return item.caseTitle?.toLowerCase().includes(search) || item.caseType?.toLowerCase().includes(search);
                                                if (managementTab === 'appointments') return item.userFullName?.toLowerCase().includes(search) || item.lawyerFullName?.toLowerCase().includes(search);
                                                return item.fullName?.toLowerCase().includes(search) || item.username?.toLowerCase().includes(search) || item.email?.toLowerCase().includes(search);
                                            })
                                            .map((item) => (
                                                <tr key={item.id} className="group hover:bg-slate-50/50 dark:hover:bg-white/[0.02] transition-colors">
                                                    <td className="px-8 py-6">
                                                        <div className="flex items-center gap-4">
                                                            <div className="w-12 h-12 rounded-xl bg-slate-100 dark:bg-white/5 border border-white/5 flex items-center justify-center text-primary dark:text-cyber-blue font-black flex-shrink-0 group-hover:scale-110 transition-transform">
                                                                {managementTab === 'appointments' ? (
                                                                    <span className="material-symbols-outlined text-xl">event</span>
                                                                ) : managementTab === 'cases' ? (
                                                                    <span className="material-symbols-outlined text-xl">folder</span>
                                                                ) : (
                                                                    item.profilePhotoUrl ? (
                                                                        <div className="w-full h-full rounded-xl bg-cover bg-center" style={{ backgroundImage: `url(${item.profilePhotoUrl})` }}></div>
                                                                    ) : (item.username?.charAt(0).toUpperCase() || 'U')
                                                                )}
                                                            </div>
                                                            <div className="overflow-hidden">
                                                                <p className="text-sm font-black text-slate-900 dark:text-white truncate tracking-tight mb-0.5">
                                                                    {managementTab === 'appointments' ? item.userFullName : managementTab === 'cases' ? item.caseTitle : (item.fullName || item.username)}
                                                                </p>
                                                                <p className="text-[9px] text-slate-400 font-bold uppercase tracking-widest truncate">
                                                                    {managementTab === 'appointments' ? `Lawyer: ${item.lawyerFullName}` : managementTab === 'cases' ? (item.userFullName || 'Public File') : (item.email || 'Internal Identity')}
                                                                </p>
                                                            </div>
                                                        </div>
                                                    </td>
                                                    <td className="px-8 py-6">
                                                        <div className={`inline-flex px-3 py-1.5 rounded-lg text-[9px] font-black uppercase tracking-widest border backdrop-blur-md ${(item.status || item.caseStatus || (item.verified ? 'VERIFIED' : 'PENDING')).toUpperCase() === 'VERIFIED' || (item.status === 'CONFIRMED')
                                                            ? 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20'
                                                            : 'bg-amber-500/10 text-amber-500 border-amber-500/20'
                                                            }`}>
                                                            {managementTab === 'lawyers' ? (item.verified ? 'VERIFIED' : 'PENDING') : (item.status || item.caseStatus || 'ACTIVE')}
                                                        </div>
                                                    </td>
                                                    <td className="px-8 py-6">
                                                        <div className="space-y-1">
                                                            <p className="text-[11px] font-black text-slate-700 dark:text-slate-300 uppercase tracking-tight">
                                                                {managementTab === 'appointments'
                                                                    ? new Date(item.appointmentDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
                                                                    : new Date(item.createdAt || Date.now()).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
                                                                }
                                                            </p>
                                                            <p className="text-[9px] text-slate-400 font-bold uppercase tracking-widest leading-none">Record Date</p>
                                                        </div>
                                                    </td>
                                                    <td className="px-8 py-6">
                                                        <div className="flex justify-end gap-2">
                                                            {managementTab === 'cases' && item.caseStatus !== 'VERIFIED' && (
                                                                <button onClick={() => handleVerifyCase(item.id)} className="w-9 h-9 rounded-lg bg-emerald-500 text-white flex items-center justify-center hover:bg-emerald-600 shadow-lg shadow-emerald-500/20 transition-all" title="Verify">
                                                                    <span className="material-symbols-outlined text-sm">verified</span>
                                                                </button>
                                                            )}
                                                            {managementTab === 'lawyers' && !item.verified && (
                                                                <button onClick={() => handleVerifyLawyer(item.id)} className="w-9 h-9 rounded-lg bg-emerald-500 text-white flex items-center justify-center hover:bg-emerald-600 shadow-lg shadow-emerald-500/20 transition-all" title="Verify">
                                                                    <span className="material-symbols-outlined text-sm">verified</span>
                                                                </button>
                                                            )}
                                                            <button onClick={() => handleView(item, managementTab)} className="w-9 h-9 rounded-lg bg-white dark:bg-slate-800 text-slate-400 hover:text-primary dark:hover:text-cyber-blue shadow-lg flex items-center justify-center transition-all border border-slate-100 dark:border-white/5" title="View Details">
                                                                <span className="material-symbols-outlined text-sm">visibility</span>
                                                            </button>
                                                            {managementTab !== 'appointments' && (
                                                                <button onClick={() => handleEdit(item, managementTab)} className="w-9 h-9 rounded-lg bg-white dark:bg-slate-800 text-slate-400 hover:text-emerald-500 shadow-lg flex items-center justify-center transition-all border border-slate-100 dark:border-white/5" title="Edit Record">
                                                                    <span className="material-symbols-outlined text-sm">edit</span>
                                                                </button>
                                                            )}
                                                            <button onClick={() => handleDelete(managementTab, item.id)} className="w-9 h-9 rounded-lg bg-white dark:bg-slate-800 text-slate-400 hover:text-rose-500 shadow-lg flex items-center justify-center transition-all border border-slate-100 dark:border-white/5" title="Delete">
                                                                <span className="material-symbols-outlined text-sm">delete</span>
                                                            </button>
                                                        </div>
                                                    </td>
                                                </tr>
                                            ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        {/* Pagination - Floating Design */}
                        <div className="flex items-center justify-center gap-4 py-12">
                            <button
                                onClick={() => setCurrentPage(p => Math.max(0, p - 1))}
                                disabled={currentPage === 0}
                                className="w-14 h-14 glass-morphism dark:bg-white/5 rounded-2xl flex items-center justify-center text-slate-400 hover:text-primary dark:hover:text-cyber-blue transition-all disabled:opacity-20 shadow-xl border border-white/5"
                            >
                                <span className="material-symbols-outlined">navigate_before</span>
                            </button>
                            <div className="px-8 py-4 glass-morphism dark:bg-white/10 rounded-2xl">
                                <span className="text-[11px] font-black text-slate-900 dark:text-white uppercase tracking-[0.3em]">Page {currentPage + 1}</span>
                            </div>
                            <button
                                onClick={() => setCurrentPage(p => p + 1)}
                                className="w-14 h-14 glass-morphism dark:bg-white/5 rounded-2xl flex items-center justify-center text-slate-400 hover:text-primary dark:hover:text-cyber-blue transition-all shadow-xl border border-white/5"
                            >
                                <span className="material-symbols-outlined">navigate_next</span>
                            </button>
                        </div>
                    </div>
                )}
            </main>

            {/* Modal for View/Edit - Modernized */}
            {showModal && selectedItem && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center p-6" onClick={() => setShowModal(false)}>
                    <div className="absolute inset-0 bg-slate-950/60 backdrop-blur-sm"></div>
                    <div className="relative bg-white dark:bg-slate-900 w-full max-w-xl rounded-[2.5rem] shadow-2xl border border-white/20 overflow-hidden animate-in zoom-in-95 duration-300" onClick={e => e.stopPropagation()}>
                        <div className="p-10">
                            <div className="flex justify-between items-start mb-10">
                                <div>
                                    <h2 className="text-3xl font-black text-slate-900 dark:text-white tracking-tight">{editMode ? 'Edit' : 'View'} Record</h2>
                                    <p className="text-[10px] font-black text-primary dark:text-blue-400 uppercase tracking-widest mt-1">{selectedItem.type}</p>
                                </div>
                                <button onClick={() => setShowModal(false)} className="bg-slate-50 dark:bg-slate-800 p-2 rounded-xl text-slate-400 hover:text-slate-900">
                                    <span className="material-symbols-outlined">close</span>
                                </button>
                            </div>

                            <div className="space-y-6">
                                {selectedItem.type === 'users' && (
                                    <>
                                        <div className="space-y-2">
                                            <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-2">Username</label>
                                            <input className="w-full px-6 py-4 bg-slate-50 dark:bg-slate-800 rounded-2xl border border-slate-100 dark:border-slate-700 text-sm font-bold opacity-60" value={selectedItem.username || ''} disabled />
                                        </div>
                                        <div className="space-y-2">
                                            <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-2">Full Name</label>
                                            <input
                                                className="w-full px-6 py-4 bg-white dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-700 text-sm font-bold focus:ring-4 focus:ring-primary/5 outline-none transition-all"
                                                value={selectedItem.fullName || ''}
                                                onChange={e => setSelectedItem({ ...selectedItem, fullName: e.target.value })}
                                                disabled={!editMode}
                                            />
                                        </div>
                                        <div className="space-y-2">
                                            <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-2">Email</label>
                                            <input
                                                className="w-full px-6 py-4 bg-white dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-700 text-sm font-bold focus:ring-4 focus:ring-primary/5 outline-none transition-all"
                                                value={selectedItem.email || ''}
                                                onChange={e => setSelectedItem({ ...selectedItem, email: e.target.value })}
                                                disabled={!editMode}
                                            />
                                        </div>
                                    </>
                                )}

                                {selectedItem.type === 'lawyers' && (
                                    <>
                                        <div className="grid grid-cols-2 gap-4">
                                            <div className="space-y-2">
                                                <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-2">Full Name</label>
                                                <input className="w-full px-6 py-4 bg-white dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-700 text-sm font-bold" value={selectedItem.fullName || ''} onChange={e => setSelectedItem({ ...selectedItem, fullName: e.target.value })} disabled={!editMode} />
                                            </div>
                                            <div className="space-y-2">
                                                <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-2">Specialization</label>
                                                <input className="w-full px-6 py-4 bg-white dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-700 text-sm font-bold" value={selectedItem.specialization || ''} onChange={e => setSelectedItem({ ...selectedItem, specialization: e.target.value })} disabled={!editMode} />
                                            </div>
                                        </div>
                                        <div className="space-y-2">
                                            <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-2">Email</label>
                                            <input className="w-full px-6 py-4 bg-white dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-700 text-sm font-bold" value={selectedItem.email || ''} onChange={e => setSelectedItem({ ...selectedItem, email: e.target.value })} disabled={!editMode} />
                                        </div>
                                        <div className="space-y-2">
                                            <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-2">Bar Number</label>
                                            <input className="w-full px-6 py-4 bg-white dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-700 text-sm font-bold" value={selectedItem.barNumber || ''} onChange={e => setSelectedItem({ ...selectedItem, barNumber: e.target.value })} disabled={!editMode} />
                                        </div>
                                    </>
                                )}

                                {selectedItem.type === 'cases' && (
                                    <>
                                        <div className="space-y-2">
                                            <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-2">Case Title</label>
                                            <input className="w-full px-6 py-4 bg-white dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-700 text-sm font-bold" value={selectedItem.caseTitle || ''} onChange={e => setSelectedItem({ ...selectedItem, caseTitle: e.target.value })} disabled={!editMode} />
                                        </div>
                                        <div className="grid grid-cols-2 gap-4">
                                            <div className="space-y-2">
                                                <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-2">Category</label>
                                                <input className="w-full px-6 py-4 bg-white dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-700 text-sm font-bold" value={selectedItem.caseType || ''} onChange={e => setSelectedItem({ ...selectedItem, caseType: e.target.value })} disabled={!editMode} />
                                            </div>
                                            <div className="space-y-2">
                                                <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-2">Status</label>
                                                <select
                                                    className="w-full px-6 py-4 bg-white dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-700 text-sm font-bold"
                                                    value={selectedItem.caseStatus || 'OPEN'}
                                                    onChange={e => setSelectedItem({ ...selectedItem, caseStatus: e.target.value })}
                                                    disabled={!editMode}
                                                >
                                                    <option value="OPEN">Open</option>
                                                    <option value="IN_PROGRESS">In Progress</option>
                                                    <option value="CLOSED">Closed</option>
                                                    <option value="ON_HOLD">On Hold</option>
                                                    <option value="VERIFIED">Verified</option>
                                                </select>
                                            </div>
                                        </div>
                                    </>
                                )}
                            </div>

                            <div className="mt-12 flex items-center gap-4">
                                {editMode ? (
                                    <button onClick={handleSave} className="flex-1 py-5 bg-[#1a405b] text-white rounded-[2rem] text-xs font-black uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-[1.02] transition-all">Save Changes</button>
                                ) : (
                                    <button onClick={() => setEditMode(true)} className="flex-1 py-5 bg-[#1a405b] text-white rounded-[2rem] text-xs font-black uppercase tracking-widest">Edit Record</button>
                                )}
                                <button onClick={() => setShowModal(false)} className="px-10 py-5 bg-slate-50 dark:bg-slate-800 text-slate-500 rounded-[2rem] text-xs font-black uppercase tracking-widest hover:bg-slate-100 transition-all">Close</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminDashboard;
