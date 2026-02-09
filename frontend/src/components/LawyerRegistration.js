import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../utils/api';
import './Login.css'; // Reusing Login styles for consistency

const SPECIALIZATIONS = [
    { value: 'CRIMINAL', label: 'Criminal Law' },
    { value: 'CIVIL', label: 'Civil Law' },
    { value: 'FAMILY', label: 'Family Law' },
    { value: 'CORPORATE', label: 'Corporate Law' },
    { value: 'INTELLECTUAL_PROPERTY', label: 'Intellectual Property' },
    { value: 'REAL_ESTATE', label: 'Real Estate' },
    { value: 'PROPERTY', label: 'Property Law' },
    { value: 'LABOUR', label: 'Labour Law' },
    { value: 'TAX', label: 'Tax Law' },
    { value: 'CYBER_CRIME', label: 'Cyber Crime' },
    { value: 'OTHER', label: 'Other' }
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
    const navigate = useNavigate();

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSpecializationChange = (e) => {
        const { value, checked } = e.target;
        setFormData(prev => {
            if (checked) {
                return { ...prev, specializations: [...prev.specializations, value] };
            } else {
                return { ...prev, specializations: prev.specializations.filter(s => s !== value) };
            }
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        if (formData.password !== formData.confirmPassword) {
            toast.error("Passwords do not match");
            setLoading(false);
            return;
        }

        if (formData.specializations.length === 0) {
            toast.error("Please select at least one specialization");
            setLoading(false);
            return;
        }

        try {
            const payload = {
                username: formData.username,
                password: formData.password,
                fullName: formData.fullName,
                email: formData.email,
                barNumber: formData.barNumber,
                specializations: formData.specializations
            };

            await api.post('/auth/lawyer/register', payload);
            toast.success('Registration successful! Please login.');
            navigate('/lawyer-login');
        } catch (err) {
            console.error('Registration error:', err);
            toast.error(err.response?.data?.message || 'Registration failed. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-card" style={{ maxWidth: '600px' }}>
                <h1 className="login-title">Lawyer Registration</h1>
                <form onSubmit={handleSubmit} className="login-form">
                    <div className="form-group">
                        <label>Full Name</label>
                        <input
                            type="text"
                            name="fullName"
                            value={formData.fullName}
                            onChange={handleChange}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Email</label>
                        <input
                            type="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Bar Number</label>
                        <input
                            type="text"
                            name="barNumber"
                            value={formData.barNumber}
                            onChange={handleChange}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Specializations</label>
                        <div className="specializations-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginTop: '5px' }}>
                            {SPECIALIZATIONS.map(spec => (
                                <label key={spec.value} style={{ display: 'flex', alignItems: 'center', fontSize: '0.9em' }}>
                                    <input
                                        type="checkbox"
                                        value={spec.value}
                                        checked={formData.specializations.includes(spec.value)}
                                        onChange={handleSpecializationChange}
                                        style={{ marginRight: '8px' }}
                                    />
                                    {spec.label}
                                </label>
                            ))}
                        </div>
                    </div>
                    <div className="form-group">
                        <label>Username</label>
                        <input
                            type="text"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            required
                        />
                    </div>
                    <div className="row" style={{ display: 'flex', gap: '15px' }}>
                        <div className="form-group" style={{ flex: 1 }}>
                            <label>Password</label>
                            <input
                                type="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                required
                            />
                        </div>
                        <div className="form-group" style={{ flex: 1 }}>
                            <label>Confirm Password</label>
                            <input
                                type="password"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                required
                            />
                        </div>
                    </div>

                    <button type="submit" className="login-button" disabled={loading}>
                        {loading ? 'Registering...' : 'Register'}
                    </button>
                </form>
                <div className="login-footer">
                    <p>Already have an account? <Link to="/lawyer-login">Login here</Link></p>
                </div>
            </div>
        </div>
    );
}

export default LawyerRegistration;
