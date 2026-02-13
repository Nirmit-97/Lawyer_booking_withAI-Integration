import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../utils/api';

const CASE_TYPES = [
    "CRIMINAL", "FAMILY", "CIVIL", "CORPORATE", "PROPERTY", "CYBER_CRIME", "LABOUR"
];

function LawyerRegistration() {
    const [formData, setFormData] = useState({
        username: '',
        password: '',
        confirmPassword: '',
        fullName: '',
        email: '',
        barNumber: '',
        specializations: []
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSpecToggle = (spec) => {
        setFormData(prev => {
            const next = prev.specializations.includes(spec)
                ? prev.specializations.filter(s => s !== spec)
                : [...prev.specializations, spec];
            return { ...prev, specializations: next };
        });
    };

    const validateForm = () => {
        if (!formData.username.trim()) return 'Username is required';
        if (formData.username.length < 3) return 'Username must be at least 3 characters';

        const password = formData.password;
        if (!password) return 'Password is required';
        if (password.length < 8) return 'Password must be at least 8 characters long';
        if (!/[A-Z]/.test(password)) return 'Password must contain at least one uppercase letter';
        if (!/[a-z]/.test(password)) return 'Password must contain at least one lowercase letter';
        if (!/[0-9]/.test(password)) return 'Password must contain at least one digit';
        if (!/[^A-Za-z0-9]/.test(password)) return 'Password must contain at least one special character';

        if (formData.password !== formData.confirmPassword) return 'Passwords do not match';
        if (!formData.fullName.trim()) return 'Full name is required';
        if (!formData.email.trim()) return 'Email is required';
        if (!formData.barNumber.trim()) return 'Bar number is required';
        if (formData.specializations.length === 0) return 'Please select at least one specialization';

        return null;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        const validationError = validateForm();
        if (validationError) {
            setError(validationError);
            return;
        }

        setLoading(true);

        try {
            const response = await api.post('/auth/lawyer/register', {
                username: formData.username.trim(),
                password: formData.password,
                fullName: formData.fullName.trim(),
                email: formData.email.trim(),
                barNumber: formData.barNumber.trim(),
                specializations: formData.specializations
            });

            if (response.data.success) {
                toast.success('Registration successful! Awaiting admin verification.');
                navigate('/lawyer-login');
            } else {
                setError(response.data.message || 'Registration failed');
            }
        } catch (err) {
            console.error('Registration error:', err);
            setError(err.response?.data?.message || 'Error connecting to server');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-background-light dark:bg-background-dark flex items-center justify-center p-6 geo-pattern hero-gradient">
            <div className="glass-card w-full max-w-2xl rounded-[2.5rem] p-10 border border-white/50 shadow-2xl relative overflow-hidden">
                <div className="absolute top-0 right-0 w-40 h-40 bg-primary/10 rounded-full blur-3xl -mr-20 -mt-20"></div>

                <div className="flex flex-col items-center mb-8 text-center">
                    <div className="w-16 h-16 bg-primary rounded-2xl flex items-center justify-center text-white mb-4 shadow-lg shadow-primary/20">
                        <span className="material-symbols-outlined text-3xl">how_to_reg</span>
                    </div>
                    <h1 className="text-3xl font-black text-primary dark:text-white tracking-tight uppercase">Lawyer Onboarding</h1>
                    <p className="text-sm text-gray-500 font-bold mt-1 uppercase tracking-widest leading-relaxed">Join the Elite Legal Network</p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="space-y-2">
                            <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Expert Username *</label>
                            <input
                                type="text"
                                name="username"
                                value={formData.username}
                                onChange={handleChange}
                                placeholder="Username"
                                className="w-full px-6 py-4 bg-white/50 border-2 border-transparent rounded-2xl focus:border-primary focus:bg-white transition-all outline-none font-bold text-primary"
                                required
                            />
                        </div>

                        <div className="space-y-2">
                            <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Full Legal Name *</label>
                            <input
                                type="text"
                                name="fullName"
                                value={formData.fullName}
                                onChange={handleChange}
                                placeholder="Full Name"
                                className="w-full px-6 py-4 bg-white/50 border-2 border-transparent rounded-2xl focus:border-primary focus:bg-white transition-all outline-none font-bold text-primary"
                                required
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="space-y-2">
                            <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Bar Council Number *</label>
                            <input
                                type="text"
                                name="barNumber"
                                value={formData.barNumber}
                                onChange={handleChange}
                                placeholder="E.g. MAH/123/2024"
                                className="w-full px-6 py-4 bg-white/50 border-2 border-transparent rounded-2xl focus:border-primary focus:bg-white transition-all outline-none font-bold text-primary"
                                required
                            />
                        </div>

                        <div className="space-y-2">
                            <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Official Email *</label>
                            <input
                                type="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                placeholder="Email Address"
                                className="w-full px-6 py-4 bg-white/50 border-2 border-transparent rounded-2xl focus:border-primary focus:bg-white transition-all outline-none font-bold text-primary"
                                required
                            />
                        </div>
                    </div>

                    <div className="space-y-4">
                        <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Expertise Matrix *</label>
                        <div className="flex flex-wrap gap-2">
                            {CASE_TYPES.map(type => (
                                <button
                                    key={type}
                                    type="button"
                                    onClick={() => handleSpecToggle(type)}
                                    className={`px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest border transition-all ${formData.specializations.includes(type)
                                        ? 'bg-primary text-white border-primary shadow-lg scale-105'
                                        : 'bg-white/50 text-gray-400 border-gray-100 dark:border-white/10 hover:border-primary'
                                        }`}
                                >
                                    {type.replace('_', ' ')}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="space-y-2">
                            <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Security Secret *</label>
                            <input
                                type="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                placeholder="Password"
                                className="w-full px-6 py-4 bg-white/50 border-2 border-transparent rounded-2xl focus:border-primary focus:bg-white transition-all outline-none font-bold text-primary"
                                required
                            />
                            <p className="px-4 text-[9px] font-bold text-gray-400 uppercase tracking-wider">
                                Min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special
                            </p>
                        </div>

                        <div className="space-y-2">
                            <label className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 ml-4">Confirm Secret *</label>
                            <input
                                type="password"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                placeholder="Confirm"
                                className="w-full px-6 py-4 bg-white/50 border-2 border-transparent rounded-2xl focus:border-primary focus:bg-white transition-all outline-none font-bold text-primary"
                                required
                            />
                        </div>
                    </div>

                    {error && (
                        <div className="bg-red-500/10 border border-red-500/20 text-red-500 text-xs font-bold p-4 rounded-xl flex items-center gap-2">
                            <span className="material-symbols-outlined text-sm">report_problem</span>
                            {error}
                        </div>
                    )}

                    <button
                        type="submit"
                        className="w-full h-16 bg-primary text-white rounded-2xl font-black text-lg shadow-xl shadow-primary/30 hover:scale-[1.02] transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                        disabled={loading}
                    >
                        {loading ? (
                            <span className="animate-spin material-symbols-outlined">refresh</span>
                        ) : (
                            <>
                                Initialize Onboarding
                                <span className="material-symbols-outlined">rocket_launch</span>
                            </>
                        )}
                    </button>
                </form>

                <div className="mt-8 pt-8 border-t border-gray-100 text-center">
                    <p className="text-sm font-bold text-gray-500">
                        Already registered as counsel? <Link to="/lawyer-login" className="text-primary hover:underline">Sign In Here</Link>
                    </p>
                </div>
            </div>
        </div>
    );
}

export default LawyerRegistration;
