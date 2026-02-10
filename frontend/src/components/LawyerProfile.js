import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { lawyersApi } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import { toast } from 'react-toastify';

const LawyerProfile = ({ lawyerId: propLawyerId, onUpdate }) => {
  const { id: paramLawyerId } = useParams();
  const lawyerId = propLawyerId || paramLawyerId;
  const navigate = useNavigate();

  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({});
  const [saving, setSaving] = useState(false);

  const isOwnProfile = user?.role === 'lawyer' && parseInt(user?.id) === parseInt(lawyerId);

  const categories = ["Criminal", "Family", "Civil", "Corporate", "Property", "Cyber Crime", "Labour"];

  const fetchProfile = useCallback(async () => {
    try {
      setLoading(true);
      const response = await lawyersApi.getProfile(lawyerId);
      setProfile(response.data);
      setFormData(response.data);
    } catch (err) {
      console.error(`Error fetching lawyer profile:`, err);
      toast.error("Failed to load profile");
    } finally {
      setLoading(false);
    }
  }, [lawyerId]);

  useEffect(() => {
    if (lawyerId) {
      fetchProfile();
    }
  }, [lawyerId, fetchProfile]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const toggleCategory = (cat) => {
    let specsArray = [];
    if (Array.isArray(formData.specializations)) {
      specsArray = formData.specializations;
    } else if (typeof formData.specializations === 'string') {
      specsArray = formData.specializations.split(',').map(s => s.trim()).filter(Boolean);
    }

    let newSpecs;
    if (specsArray.includes(cat)) {
      newSpecs = specsArray.filter(s => s !== cat).join(', ');
    } else {
      newSpecs = [...specsArray, cat].join(', ');
    }

    setFormData(prev => ({
      ...prev,
      specializations: newSpecs,
      specialization: newSpecs.split(',')[0] || "" // Set primary specialization to first one
    }));
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      const response = await lawyersApi.updateProfile(lawyerId, formData);
      setProfile(response.data);
      setIsEditing(false);
      toast.success("Profile updated successfully!");
      if (onUpdate) onUpdate(response.data);
    } catch (err) {
      console.error("Error updating profile:", err);
      toast.error("Failed to update profile");
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div style={{ textAlign: 'center', padding: '50px' }}>Loading profile...</div>;
  if (!profile) return <div style={{ textAlign: 'center', padding: '50px' }}>Lawyer profile not found.</div>;

  const styles = {
    container: {
      padding: '30px',
      backgroundColor: '#ffffff',
      borderRadius: '20px',
      boxShadow: '0 10px 30px rgba(0,0,0,0.1)',
      maxWidth: '700px',
      margin: '40px auto',
      fontFamily: "'Outfit', 'Inter', sans-serif",
      position: 'relative'
    },
    header: {
      display: 'flex',
      alignItems: 'center',
      gap: '24px',
      marginBottom: '30px'
    },
    avatar: {
      width: '100px',
      height: '100px',
      borderRadius: '50%',
      backgroundColor: '#f0f4f8',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontSize: '40px',
      color: '#3498db',
      fontWeight: 'bold',
      border: '4px solid #fff',
      boxShadow: '0 4px 10px rgba(0,0,0,0.1)'
    },
    name: { margin: 0, fontSize: '28px', color: '#1a2a3a', fontWeight: '800' },
    specializationBadge: {
      display: 'inline-block',
      padding: '6px 14px',
      backgroundColor: '#e1f5fe',
      color: '#0288d1',
      borderRadius: '20px',
      fontSize: '14px',
      fontWeight: '700',
      marginTop: '8px'
    },
    editButton: {
      position: 'absolute',
      top: '30px',
      right: '30px',
      padding: '10px 20px',
      backgroundColor: isEditing ? '#f44336' : '#3498db',
      color: 'white',
      border: 'none',
      borderRadius: '12px',
      cursor: 'pointer',
      fontWeight: '600',
      transition: 'all 0.3s ease',
      boxShadow: '0 4px 12px rgba(52, 152, 219, 0.2)'
    },
    formGroup: { marginBottom: '20px' },
    label: {
      display: 'block',
      fontSize: '13px',
      color: '#7f8c8d',
      fontWeight: '700',
      textTransform: 'uppercase',
      letterSpacing: '1px',
      marginBottom: '8px'
    },
    input: {
      width: '100%',
      padding: '12px 16px',
      borderRadius: '12px',
      border: '2px solid #edeff2',
      fontSize: '15px',
      outline: 'none',
      transition: 'border-color 0.3s',
      boxSizing: 'border-box'
    },
    categoryContainer: {
      display: 'flex',
      flexWrap: 'wrap',
      gap: '10px',
      marginTop: '10px'
    },
    categoryChip: (selected) => ({
      padding: '8px 16px',
      borderRadius: '12px',
      fontSize: '14px',
      fontWeight: '600',
      cursor: 'pointer',
      border: '2px solid',
      borderColor: selected ? '#3498db' : '#edeff2',
      backgroundColor: selected ? '#ebf5fb' : '#fff',
      color: selected ? '#3498db' : '#546e7a',
      transition: 'all 0.2s ease'
    }),
    saveButton: {
      width: '100%',
      padding: '14px',
      backgroundColor: '#2ecc71',
      color: 'white',
      border: 'none',
      borderRadius: '12px',
      fontSize: '16px',
      fontWeight: '700',
      cursor: 'pointer',
      marginTop: '20px',
      boxShadow: '0 6px 18px rgba(46, 204, 113, 0.2)'
    }
  };

  return (
    <div style={styles.container}>
      {isOwnProfile && (
        <button
          style={styles.editButton}
          onClick={() => setIsEditing(!isEditing)}
        >
          {isEditing ? 'Cancel' : 'Edit Profile'}
        </button>
      )}

      {!isEditing ? (
        <>
          <div style={styles.header}>
            <div style={styles.avatar}>
              {profile.profilePhotoUrl ? (
                <img src={profile.profilePhotoUrl} alt={profile.fullName} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
              ) : (
                profile.fullName?.charAt(0) || 'L'
              )}
            </div>
            <div>
              <h2 style={styles.name}>{profile.fullName}</h2>
              <div style={styles.specializationBadge}>{profile.specializations || profile.specialization || 'General Practitioner'}</div>
              <div style={{ color: '#f1c40f', marginTop: '10px', fontSize: '15px', fontWeight: 'bold' }}>
                {'★'.repeat(5)}
                <span style={{ color: '#95a5a6', fontSize: '13px', marginLeft: '8px', fontWeight: '500' }}>
                  ({profile.rating?.toFixed(1) || '5.0'} Rating • {profile.completedCasesCount || 0} cases)
                </span>
              </div>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '25px', marginTop: '30px', padding: '25px', backgroundColor: '#fcfdfe', borderRadius: '16px' }}>
            <div>
              <div style={styles.label}>Experience</div>
              <div style={{ fontSize: '18px', fontWeight: '700', color: '#2c3e50' }}>{profile.yearsOfExperience || '5+'} Years</div>
            </div>
            <div>
              <div style={styles.label}>Languages</div>
              <div style={{ fontSize: '18px', fontWeight: '700', color: '#2c3e50' }}>{profile.languagesKnown || 'English, Gujarati'}</div>
            </div>
            <div>
              <div style={styles.label}>Bar Number</div>
              <div style={{ fontSize: '18px', fontWeight: '700', color: '#2c3e50' }}>{profile.barNumber || 'N/A'}</div>
            </div>
            <div>
              <div style={styles.label}>Availability</div>
              <div style={{ fontSize: '18px', fontWeight: '700', color: '#2c3e50' }}>{profile.availabilityInfo || 'Mon - Fri'}</div>
            </div>
          </div>

          <div style={{ marginTop: '30px', padding: '20px' }}>
            <div style={styles.label}>Contact & Email</div>
            <div style={{ fontSize: '16px', color: '#3498db', fontWeight: '600' }}>{profile.email}</div>
          </div>
        </>
      ) : (
        <div style={{ marginTop: '40px' }}>
          <h2 style={{ ...styles.name, marginBottom: '25px' }}>Edit Your Profile</h2>

          <div style={styles.formGroup}>
            <label style={styles.label}>Full Name</label>
            <input
              style={styles.input}
              name="fullName"
              value={formData.fullName || ""}
              onChange={handleInputChange}
            />
          </div>

          <div style={styles.formGroup}>
            <label style={styles.label}>Legal Specializations (Select all that apply)</label>
            <div style={styles.categoryContainer}>
              {categories.map(cat => {
                const specs = formData.specializations || [];
                const isSelected = Array.isArray(specs)
                  ? specs.some(s => s.toUpperCase().replace(/ /g, '_') === cat.toUpperCase().replace(/ /g, '_'))
                  : specs.toUpperCase().includes(cat.toUpperCase());
                return (
                  <div
                    key={cat}
                    style={styles.categoryChip(isSelected)}
                    onClick={() => toggleCategory(cat)}
                  >
                    {isSelected ? '✓ ' : '+ '}{cat}
                  </div>
                );
              })}
            </div>
            <p style={{ fontSize: '12px', color: '#95a5a6', marginTop: '10px' }}>
              Selected: {formData.specializations || "None"}
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
            <div style={styles.formGroup}>
              <label style={styles.label}>Years of Experience</label>
              <input
                type="number"
                style={styles.input}
                name="yearsOfExperience"
                value={formData.yearsOfExperience || ""}
                onChange={handleInputChange}
              />
            </div>
            <div style={styles.formGroup}>
              <label style={styles.label}>Languages Known</label>
              <input
                style={styles.input}
                name="languagesKnown"
                placeholder="e.g. English, Hindi, Gujarati"
                value={formData.languagesKnown || ""}
                onChange={handleInputChange}
              />
            </div>
          </div>

          <div style={styles.formGroup}>
            <label style={styles.label}>Availability Info</label>
            <input
              style={styles.input}
              name="availabilityInfo"
              placeholder="e.g. Mon-Fri, 9AM-5PM"
              value={formData.availabilityInfo || ""}
              onChange={handleInputChange}
            />
          </div>

          <div style={styles.formGroup}>
            <label style={styles.label}>UPI ID (For Payouts)</label>
            <input
              style={styles.input}
              name="upiId"
              placeholder="e.g. mobile@upi"
              value={formData.upiId || ""}
              onChange={handleInputChange}
            />
          </div>

          <div style={styles.formGroup}>
            <label style={styles.label}>Profile Photo URL</label>
            <input
              style={styles.input}
              name="profilePhotoUrl"
              value={formData.profilePhotoUrl || ""}
              onChange={handleInputChange}
            />
          </div>

          <button
            style={styles.saveButton}
            onClick={handleSave}
            disabled={saving}
          >
            {saving ? 'Saving Changes...' : 'Save Profile Updates'}
          </button>
        </div>
      )}
    </div>
  );
};

export default LawyerProfile;
