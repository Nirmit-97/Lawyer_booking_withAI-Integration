import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminApi } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import { toast } from 'react-toastify';
import './AdminDashboard.css';

const AdminDashboard = () => {
    const [activeTab, setActiveTab] = useState('dashboard');
    const [stats, setStats] = useState({ totalUsers: 0, totalLawyers: 0, totalCases: 0 });
    const [users, setUsers] = useState([]);
    const [lawyers, setLawyers] = useState([]);
    const [cases, setCases] = useState([]);
    const [loading, setLoading] = useState(false);
    const [statsLoading, setStatsLoading] = useState(false);
    const [selectedItem, setSelectedItem] = useState(null);
    const [showModal, setShowModal] = useState(false);
    const [editMode, setEditMode] = useState(false);
    const [newLawyerId, setNewLawyerId] = useState('');

    const { user, logout, loading: authLoading } = useAuth();
    const navigate = useNavigate();

    // Fetch dashboard stats
    useEffect(() => {
        if (user && user.role === 'admin') {
            fetchStats();
        }
    }, [user]);

    // Fetch data based on active tab
    useEffect(() => {
        if (!user || user.role !== 'admin') return;

        if (activeTab === 'users') fetchUsers();
        else if (activeTab === 'lawyers') fetchLawyers();
        else if (activeTab === 'cases') fetchCases();
    }, [activeTab, user]);

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

    const fetchUsers = async () => {
        setLoading(true);
        try {
            const response = await adminApi.getUsers();
            setUsers(response.data.content || []);
        } catch (error) {
            console.error('Error fetching users:', error);
            handleError(error);
        } finally {
            setLoading(false);
        }
    };

    const fetchLawyers = async () => {
        setLoading(true);
        try {
            const response = await adminApi.getLawyers();
            setLawyers(response.data.content || []);
        } catch (error) {
            console.error('Error fetching lawyers:', error);
            handleError(error);
        } finally {
            setLoading(false);
        }
    };

    const fetchCases = async () => {
        setLoading(true);
        try {
            const response = await adminApi.getCases();
            setCases(response.data.content || []);
        } catch (error) {
            console.error('Error fetching cases:', error);
            handleError(error);
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (type, id) => {
        if (!window.confirm(`Are you sure you want to delete this ${type}?`)) return;

        try {
            if (type === 'users') await adminApi.deleteUser(id);
            else if (type === 'lawyers') await adminApi.deleteLawyer(id);
            else if (type === 'cases') await adminApi.deleteCase(id);

            toast.success(`${type} deleted successfully`);

            if (type === 'users') fetchUsers();
            else if (type === 'lawyers') fetchLawyers();
            else if (type === 'cases') fetchCases();
            fetchStats();
        } catch (error) {
            console.error(`Error deleting ${type}:`, error);
            toast.error(`Failed to delete ${type}`);
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

    const handleLogout = () => {
        logout();
        navigate('/admin/login');
    };

    const handleVerifyCase = async (caseId) => {
        try {
            await adminApi.updateCase(caseId, { caseStatus: 'VERIFIED' });
            toast.success('Case verified successfully');
            fetchCases();
            fetchStats();
        } catch (error) {
            console.error('Error verifying case:', error);
            toast.error('Failed to verify case');
        }
    };

    const handleError = (error) => {
        if (error.response?.status === 401 || error.response?.status === 403) {
            logout();
            navigate('/admin/login');
        } else {
            toast.error('Operation failed. Check console for details.');
        }
    };

    if (authLoading) {
        return (
            <div className="admin-dashboard-loading">
                <div className="loader">Loading Admin Session...</div>
            </div>
        );
    }

    if (!user || user.role !== 'admin') {
        return null; // ProtectedRoute will handle redirect
    }

    return (
        <div className="admin-dashboard">
            <header className="admin-header">
                <div>
                    <h1>Admin Dashboard</h1>
                    <p className="welcome-text">Logged in as: {user.fullName || user.username}</p>
                </div>
                <button onClick={handleLogout} className="logout-btn">Logout</button>
            </header>

            <nav className="admin-nav">
                <button
                    className={activeTab === 'dashboard' ? 'active' : ''}
                    onClick={() => setActiveTab('dashboard')}
                >
                    Dashboard
                </button>
                <button
                    className={activeTab === 'users' ? 'active' : ''}
                    onClick={() => setActiveTab('users')}
                >
                    Users
                </button>
                <button
                    className={activeTab === 'lawyers' ? 'active' : ''}
                    onClick={() => setActiveTab('lawyers')}
                >
                    Lawyers
                </button>
                <button
                    className={activeTab === 'cases' ? 'active' : ''}
                    onClick={() => setActiveTab('cases')}
                >
                    Cases
                </button>
            </nav>

            <div className="admin-content">
                {activeTab === 'dashboard' && (
                    <div className="stats-container">
                        {statsLoading ? (
                            <div className="loading-stats">Updating statistics...</div>
                        ) : (
                            <div className="stats-grid">
                                <div className="stat-card">
                                    <h3>Total Users</h3>
                                    <p className="stat-number">{stats.totalUsers}</p>
                                </div>
                                <div className="stat-card">
                                    <h3>Total Lawyers</h3>
                                    <p className="stat-number">{stats.totalLawyers}</p>
                                </div>
                                <div className="stat-card">
                                    <h3>Total Cases</h3>
                                    <p className="stat-number">{stats.totalCases}</p>
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'users' && (
                    <div className="data-section">
                        <h2>Users Management</h2>
                        {loading ? <p>Loading...</p> : (
                            <table className="data-table">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Username</th>
                                        <th>Full Name</th>
                                        <th>Email</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {users.map(user => (
                                        <tr key={user.id}>
                                            <td>{user.id}</td>
                                            <td>{user.username}</td>
                                            <td>{user.fullName}</td>
                                            <td>{user.email}</td>
                                            <td>
                                                <button onClick={() => handleView(user, 'users')} className="btn-view">View</button>
                                                <button onClick={() => handleEdit(user, 'users')} className="btn-edit">Edit</button>
                                                <button onClick={() => handleDelete('users', user.id)} className="btn-delete">Delete</button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}

                {activeTab === 'lawyers' && (
                    <div className="data-section">
                        <h2>Lawyers Management</h2>
                        {loading ? <p>Loading...</p> : (
                            <table className="data-table">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Username</th>
                                        <th>Full Name</th>
                                        <th>Email</th>
                                        <th>Specialization</th>
                                        <th>Bar Number</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {lawyers.map(lawyer => (
                                        <tr key={lawyer.id}>
                                            <td>{lawyer.id}</td>
                                            <td>{lawyer.username}</td>
                                            <td>{lawyer.fullName}</td>
                                            <td>{lawyer.email}</td>
                                            <td>{lawyer.specialization}</td>
                                            <td>{lawyer.barNumber}</td>
                                            <td>
                                                <button onClick={() => handleView(lawyer, 'lawyers')} className="btn-view">View</button>
                                                <button onClick={() => handleEdit(lawyer, 'lawyers')} className="btn-edit">Edit</button>
                                                <button onClick={() => handleDelete('lawyers', lawyer.id)} className="btn-delete">Delete</button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}

                {activeTab === 'cases' && (
                    <div className="data-section">
                        <h2>Cases Management</h2>
                        {loading ? <p>Loading...</p> : (
                            <table className="data-table">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Title</th>
                                        <th>Category</th>
                                        <th>Status</th>
                                        <th>User ID</th>
                                        <th>Lawyer ID</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {cases.map(caseItem => (
                                        <tr key={caseItem.id}>
                                            <td>{caseItem.id}</td>
                                            <td>{caseItem.caseTitle}</td>
                                            <td>{caseItem.caseCategory}</td>
                                            <td>{caseItem.caseStatus}</td>
                                            <td>{caseItem.userId}</td>
                                            <td>{caseItem.lawyerId || 'Unassigned'}</td>
                                            <td>
                                                <button onClick={() => handleView(caseItem, 'cases')} className="btn-view">View</button>
                                                <button onClick={() => handleEdit(caseItem, 'cases')} className="btn-edit">Edit</button>
                                                {caseItem.caseStatus !== 'VERIFIED' && (
                                                    <button onClick={() => handleVerifyCase(caseItem.id)} className="btn-verify">Verify</button>
                                                )}
                                                <button onClick={() => handleReassignCase(caseItem.id)} className="btn-reassign">Reassign</button>
                                                <button onClick={() => handleDelete('cases', caseItem.id)} className="btn-delete">Delete</button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}
            </div>

            {showModal && selectedItem && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <h2>{editMode ? 'Edit' : 'View'} {selectedItem.type}</h2>
                        <div className="modal-body">
                            {selectedItem.type === 'users' && (
                                <>
                                    <div className="form-group">
                                        <label>Username:</label>
                                        <input type="text" value={selectedItem.username || ''} disabled />
                                    </div>
                                    <div className="form-group">
                                        <label>Full Name:</label>
                                        <input
                                            type="text"
                                            value={selectedItem.fullName || ''}
                                            onChange={(e) => setSelectedItem({ ...selectedItem, fullName: e.target.value })}
                                            disabled={!editMode}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Email:</label>
                                        <input
                                            type="email"
                                            value={selectedItem.email || ''}
                                            onChange={(e) => setSelectedItem({ ...selectedItem, email: e.target.value })}
                                            disabled={!editMode}
                                        />
                                    </div>
                                </>
                            )}

                            {selectedItem.type === 'lawyers' && (
                                <>
                                    <div className="form-group">
                                        <label>Username:</label>
                                        <input type="text" value={selectedItem.username || ''} disabled />
                                    </div>
                                    <div className="form-group">
                                        <label>Full Name:</label>
                                        <input
                                            type="text"
                                            value={selectedItem.fullName || ''}
                                            onChange={(e) => setSelectedItem({ ...selectedItem, fullName: e.target.value })}
                                            disabled={!editMode}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Email:</label>
                                        <input
                                            type="email"
                                            value={selectedItem.email || ''}
                                            onChange={(e) => setSelectedItem({ ...selectedItem, email: e.target.value })}
                                            disabled={!editMode}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Specialization:</label>
                                        <input
                                            type="text"
                                            value={selectedItem.specialization || ''}
                                            onChange={(e) => setSelectedItem({ ...selectedItem, specialization: e.target.value })}
                                            disabled={!editMode}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Bar Number:</label>
                                        <input
                                            type="text"
                                            value={selectedItem.barNumber || ''}
                                            onChange={(e) => setSelectedItem({ ...selectedItem, barNumber: e.target.value })}
                                            disabled={!editMode}
                                        />
                                    </div>
                                </>
                            )}

                            {selectedItem.type === 'cases' && (
                                <>
                                    <div className="form-group">
                                        <label>Case Title:</label>
                                        <input
                                            type="text"
                                            value={selectedItem.caseTitle || ''}
                                            onChange={(e) => setSelectedItem({ ...selectedItem, caseTitle: e.target.value })}
                                            disabled={!editMode}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Category:</label>
                                        <input
                                            type="text"
                                            value={selectedItem.caseCategory || ''}
                                            onChange={(e) => setSelectedItem({ ...selectedItem, caseCategory: e.target.value })}
                                            disabled={!editMode}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label>Status:</label>
                                        <select
                                            value={selectedItem.caseStatus || 'OPEN'}
                                            onChange={(e) => setSelectedItem({ ...selectedItem, caseStatus: e.target.value })}
                                            disabled={!editMode}
                                        >
                                            <option value="OPEN">Open</option>
                                            <option value="IN_PROGRESS">In Progress</option>
                                            <option value="CLOSED">Closed</option>
                                            <option value="ON_HOLD">On Hold</option>
                                            <option value="VERIFIED">Verified</option>
                                        </select>
                                    </div>
                                    <div className="form-group">
                                        <label>Description:</label>
                                        <textarea
                                            value={selectedItem.description || ''}
                                            onChange={(e) => setSelectedItem({ ...selectedItem, description: e.target.value })}
                                            disabled={!editMode}
                                            rows="4"
                                        />
                                    </div>
                                </>
                            )}
                        </div>
                        <div className="modal-actions">
                            {editMode && <button onClick={handleSave} className="btn-save">Save</button>}
                            <button onClick={() => setShowModal(false)} className="btn-cancel">Close</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminDashboard;
