import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { lawyersApi, reviewsApi } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import Booking from './Booking';
import { toast } from 'react-toastify';

const LawyerProfile = ({ lawyerId: propLawyerId, onUpdate }) => {
  const { id: paramLawyerId } = useParams();
  const lawyerId = propLawyerId || paramLawyerId;
  const navigate = useNavigate();
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const caseId = searchParams.get('caseId');

  const [showBookingModal, setShowBookingModal] = useState(false);

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({});
  const [saving, setSaving] = useState(false);
  const [reviews, setReviews] = useState([]);

  const isOwnProfile = user?.role === 'lawyer' && parseInt(user?.id) === parseInt(lawyerId);
  const isVerified = profile?.verified === true;
  const categories = ["Criminal", "Family", "Civil", "Corporate", "Property", "Cyber Crime", "Labour"];

  const getSpecsArray = (specs) => {
    if (Array.isArray(specs)) return specs;
    if (typeof specs === 'string') return specs.split(',').map(s => s.trim()).filter(Boolean);
    return [];
  };

  const fetchProfile = useCallback(async () => {
    try {
      setLoading(true);
      const response = await lawyersApi.getProfile(lawyerId);
      setProfile(response.data);
      setFormData(response.data);
    } catch (err) {
      toast.error("Failed to load profile");
    } finally {
      setLoading(false);
    }
  }, [lawyerId]);

  const fetchReviews = useCallback(async () => {
    try {
      const response = await reviewsApi.getByLawyer(lawyerId);
      setReviews(response.data || []);
    } catch (err) {
      console.error("Failed to load reviews:", err);
    }
  }, [lawyerId]);

  useEffect(() => {
    if (lawyerId) {
      fetchProfile();
      fetchReviews();
    }
  }, [lawyerId, fetchProfile, fetchReviews]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const toggleCategory = (cat) => {
    let specsArray = getSpecsArray(formData.specializations);
    let newSpecs = specsArray.includes(cat) ? specsArray.filter(s => s !== cat) : [...specsArray, cat];
    setFormData(prev => ({ ...prev, specializations: newSpecs, specialization: newSpecs[0] || "" }));
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      const response = await lawyersApi.updateProfile(lawyerId, formData);
      console.log("Profile Update Successful. Response Data:", response.data);
      setProfile(response.data);
      setFormData(response.data); // Keep form in sync with server state
      setIsEditing(false);
      toast.success("Profile updated!");
      if (onUpdate) onUpdate(response.data);
    } catch (err) {
      toast.error("Update failed");
    } finally {
      setSaving(false);
    }
  };

  // Case Successes
  const getSuccessesArray = (data) => {
    if (Array.isArray(data)) return data;
    if (typeof data === 'string') {
      try { return JSON.parse(data); } catch (e) { return []; }
    }
    return [];
  };

  const successes = getSuccessesArray(formData.notableSuccesses || profile?.notableSuccesses);

  useEffect(() => {
    if (profile && !profile.notableSuccesses && !formData.notableSuccesses && isOwnProfile) {
      // Seed with initial examples if truly empty and it's the own profile
      const initial = [
        { id: 1, title: "Multi-Million Dollar Land Dispute", desc: "Successfully defended a major real estate developer in a 50-acre title dispute, resulting in a favorable settlement." },
        { id: 2, title: "Corporate Merger Arbitration", desc: "Lead counsel in a high-stakes arbitration regarding a breached acquisition agreement, securing a $12M payout." }
      ];
      setFormData(prev => ({ ...prev, notableSuccesses: JSON.stringify(initial) }));
    }
  }, [profile, isOwnProfile, formData.notableSuccesses]);

  const handleDeleteSuccess = (id) => {
    const updated = successes.filter(s => s.id !== id);
    setFormData(prev => ({ ...prev, notableSuccesses: JSON.stringify(updated) }));
  };

  const handleAddSuccess = () => {
    const newRecord = {
      id: Date.now(),
      title: "New Legal Victory",
      desc: "Briefly describe the outcome and impact of this case..."
    };
    const updated = [...successes, newRecord];
    setFormData(prev => ({ ...prev, notableSuccesses: JSON.stringify(updated) }));
  };

  const handleUpdateSuccess = (id, field, value) => {
    const updated = successes.map(s => s.id === id ? { ...s, [field]: value } : s);
    setFormData(prev => ({ ...prev, notableSuccesses: JSON.stringify(updated) }));
  };

  const removeTag = (tagToRemove) => {
    const specsArray = getSpecsArray(formData.specializations);
    const newSpecs = specsArray.filter(s => s !== tagToRemove);
    setFormData(prev => ({ ...prev, specializations: newSpecs }));
  };

  const hasChanges = JSON.stringify(profile) !== JSON.stringify(formData);

  if (loading) return (
    <div className="flex flex-col items-center justify-center p-20 animate-pulse">
      <div className="w-24 h-24 bg-slate-200 dark:bg-slate-800 rounded-full mb-4" />
      <div className="h-4 w-32 bg-slate-200 dark:bg-slate-800 rounded" />
    </div>
  );

  if (!profile) return (
    <div className="p-20 text-center">
      <span className="material-symbols-outlined text-4xl text-slate-300 mb-2">person_off</span>
      <p className="text-sm font-black text-slate-400 uppercase tracking-widest">Counsel Not Found</p>
    </div>
  );

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 font-display pb-32">
      {/* Internal Edit Mode Header */}
      {isOwnProfile && (
        <header className="sticky top-0 z-50 w-full bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="max-w-[1280px] mx-auto px-6 h-16 flex items-center justify-between">
            <div className="flex items-center gap-6">
              <div className="flex items-center gap-2">
                <div className="bg-primary p-1.5 rounded-lg text-white">
                  <span className="material-symbols-outlined text-xl">balance</span>
                </div>
                <h1 className="text-xl font-bold tracking-tight text-primary dark:text-white">LegalConnect</h1>
              </div>
            </div>

            <div className="flex items-center gap-4">
              {hasChanges && (
                <div className="flex gap-2">
                  <button
                    onClick={() => {
                      setFormData(profile);
                      setIsEditing(false);
                    }}
                    className="flex items-center gap-2 px-4 py-2 text-slate-400 text-[10px] font-black hover:text-slate-600 transition-all uppercase tracking-[0.2em]"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleSave}
                    disabled={saving}
                    className="flex items-center gap-2 px-6 py-2 bg-primary text-white text-[10px] font-black rounded-xl shadow-lg shadow-primary/20 hover:scale-105 transition-all disabled:opacity-50 uppercase tracking-[0.2em]"
                  >
                    {saving ? 'Syncing...' : 'Synchronize Profile'}
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>
      )}

      <main className="max-w-[1280px] mx-auto px-6 py-8">
        {/* Navigation Header */}
        <div className="mb-8 flex items-center justify-between">
          <button
            onClick={() => {
              if (window.history.length > 2) {
                navigate(-1);
              } else if (caseId) {
                navigate(`/case/${caseId}`);
              } else {
                navigate(user?.role === 'lawyer' ? '/lawyer-dashboard' : '/user-dashboard');
              }
            }}
            className="flex items-center gap-2 text-slate-400 hover:text-primary transition-colors group"
          >
            <div className="size-8 rounded-full bg-slate-100 dark:bg-white/5 flex items-center justify-center group-hover:bg-primary/10 transition-all">
              <span className="material-symbols-outlined text-lg">arrow_back</span>
            </div>
            <span className="text-[10px] font-black uppercase tracking-widest">
              {caseId ? 'Back to Case Detail' : 'Back to Discovery'}
            </span>
          </button>

          {isOwnProfile && isEditing && (
            <span className="text-[10px] font-black text-amber-500 uppercase tracking-widest bg-amber-50 dark:bg-amber-900/20 px-4 py-2 rounded-xl border border-amber-200 dark:border-amber-800">Unsaved Changes Pending</span>
          )}
        </div>

        {/* Profile Identity Section */}
        <section className="relative mb-8 rounded-[2rem] overflow-hidden bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xl shadow-slate-200/50 dark:shadow-none animate-in fade-in slide-in-from-bottom-4 duration-700">
          <div className="h-48 w-full bg-gradient-to-r from-primary to-blue-900 relative overflow-hidden group">
            <div className="absolute inset-0 opacity-20" style={{ backgroundImage: "radial-gradient(circle at 2px 2px, white 1px, transparent 0)", backgroundSize: "24px 24px" }}></div>
            {isOwnProfile && (
              <div className="absolute inset-0 bg-black/10 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center cursor-pointer">
                <div className="bg-white/90 px-4 py-2 rounded-xl flex items-center gap-2 text-primary font-bold shadow-lg">
                  <span className="material-symbols-outlined text-xl">photo_camera</span>
                  Change Cover
                </div>
              </div>
            )}
          </div>

          <div className="px-10 pb-10 flex flex-col md:flex-row items-end gap-8 -mt-20 relative z-10">
            <div className="relative group">
              <div className="w-44 h-44 rounded-3xl border-[6px] border-white dark:border-slate-900 shadow-2xl overflow-hidden bg-white dark:bg-slate-800 flex items-center justify-center text-5xl font-black text-primary dark:text-white relative">
                {profile.profilePhotoUrl ? (
                  <img src={profile.profilePhotoUrl} alt={profile.fullName} className="w-full h-full object-cover" />
                ) : (
                  profile.fullName?.charAt(0) || 'L'
                )}

                {isOwnProfile && (
                  <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col items-center justify-center cursor-pointer text-white">
                    <span className="material-symbols-outlined text-4xl">add_a_photo</span>
                    <span className="text-[10px] font-black mt-2 uppercase tracking-widest">Update</span>
                  </div>
                )}
              </div>
              <div className="absolute -bottom-2 -right-2 bg-emerald-500 text-white p-1.5 rounded-full border-4 border-white dark:border-slate-900 shadow-lg">
                <span className="material-symbols-outlined text-sm font-black fill-icon">verified</span>
              </div>
            </div>

            <div className="flex-1 flex flex-col md:flex-row justify-between items-end pb-3">
              <div className="space-y-2 flex-1">
                <div className="flex items-center justify-between w-full">
                  <div className="flex items-center gap-3">
                    {isEditing ? (
                      <input
                        type="text"
                        name="fullName"
                        value={formData.fullName || ''}
                        onChange={handleInputChange}
                        className="text-4xl font-black text-slate-900 dark:text-white tracking-tight leading-none bg-transparent border-b-2 border-primary/20 focus:border-primary outline-none w-full max-w-lg"
                        placeholder="Enter Full Name"
                      />
                    ) : (
                      <h2 className="text-4xl font-black text-slate-900 dark:text-white tracking-tight leading-none">{profile.fullName}</h2>
                    )}
                    <span className="bg-primary/5 text-primary border border-primary/10 dark:text-blue-400 px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-widest shadow-sm whitespace-nowrap">Verified Expert</span>
                  </div>

                  {isOwnProfile && !isEditing && (
                    <button
                      onClick={() => setIsEditing(true)}
                      className="flex items-center gap-2 px-6 py-3 bg-primary text-white text-xs font-black rounded-2xl shadow-xl shadow-primary/20 hover:scale-105 transition-all uppercase tracking-widest"
                    >
                      <span className="material-symbols-outlined text-lg">edit</span>
                      Edit Profile
                    </button>
                  )}
                </div>

                {isEditing ? (
                  <input
                    type="text"
                    name="headline"
                    value={formData.headline || 'Corporate Law & Property Dispute Specialist'}
                    onChange={handleInputChange}
                    className="text-xl font-medium text-slate-500 dark:text-slate-400 bg-transparent border-b border-slate-200 dark:border-slate-800 outline-none w-full max-w-lg mt-2 focus:border-primary transition-all"
                    placeholder="Enter professional headline"
                  />
                ) : (
                  <p className="text-xl font-medium text-slate-500 dark:text-slate-400">
                    {profile.headline || 'Corporate Law & Property Dispute Specialist'}
                  </p>
                )}
                <div className="flex items-center gap-5 pt-2">
                  <span className="flex items-center gap-1.5 text-xs font-bold text-slate-400 uppercase tracking-widest">
                    <span className="material-symbols-outlined text-base">location_on</span> Mumbai, India
                  </span>
                  <span className="flex items-center gap-1.5 text-xs font-bold text-slate-400 uppercase tracking-widest">
                    <span className="material-symbols-outlined text-base">language</span>
                    {isEditing ? (
                      <div className="flex flex-wrap gap-2 py-1">
                        {['English', 'Hindi', 'Gujarati', 'Marathi', 'Sanskrit'].map(lang => (
                          <button
                            key={lang}
                            onClick={() => {
                              const current = formData.languagesKnown?.split(',').map(s => s.trim()).filter(Boolean) || [];
                              const next = current.includes(lang) ? current.filter(l => l !== lang) : [...current, lang];
                              setFormData(prev => ({ ...prev, languagesKnown: next.join(', ') }));
                            }}
                            className={`px-3 py-1 rounded-full text-[9px] font-black uppercase tracking-widest transition-all ${(formData.languagesKnown || '').includes(lang)
                              ? 'bg-primary text-white'
                              : 'bg-slate-100 dark:bg-slate-800 text-slate-400 border border-slate-200 dark:border-slate-700'
                              }`}
                          >
                            {lang}
                          </button>
                        ))}
                      </div>
                    ) : (
                      profile.languagesKnown || 'English, Hindi'
                    )}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Booking Modal */}
        {showBookingModal && (
          <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4 animate-in fade-in duration-200">
            <div className="bg-white dark:bg-slate-900 w-full max-w-4xl max-h-[90vh] overflow-y-auto rounded-[2.5rem] relative shadow-2xl">
              <button
                onClick={() => setShowBookingModal(false)}
                className="absolute top-6 right-6 z-10 size-10 rounded-full bg-slate-100 dark:bg-white/10 flex items-center justify-center text-slate-500 hover:bg-rose-500 hover:text-white transition-all"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
              <div className="p-2">
                <Booking
                  userId={user?.id}
                  userType={user?.role}
                  preSelectedLawyerId={lawyerId}
                  preSelectedLawyerName={profile?.fullName}
                  preSelectedCaseId={caseId} // Pass the caseId if available
                  onBookingSuccess={() => {
                    setShowBookingModal(false);
                    toast.success("Engagement request dispatched successfully.");
                  }}
                />
              </div>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          <article className="lg:col-span-9 space-y-8">
            {/* Bio Section */}
            <div className="bg-white dark:bg-slate-900 rounded-[2rem] p-10 border border-slate-200 dark:border-slate-800 shadow-sm relative group hover:shadow-xl hover:shadow-slate-100 dark:hover:shadow-none transition-all duration-500 animate-in fade-in slide-in-from-bottom-6 duration-700 delay-100">
              <div className="flex justify-between items-center mb-8">
                <h3 className="text-2xl font-black text-slate-900 dark:text-white flex items-center gap-3">
                  <span className="material-symbols-outlined text-primary text-3xl">person</span>
                  Professional Profile
                </h3>
              </div>
              <div className="space-y-6 text-slate-600 dark:text-slate-300 leading-loose text-lg font-medium">
                {isEditing ? (
                  <div className="space-y-4">
                    <textarea
                      name="bio"
                      value={formData.bio || ''}
                      onChange={handleInputChange}
                      className="w-full h-48 bg-slate-50 dark:bg-slate-800/50 border-2 border-primary/20 rounded-2xl p-6 focus:border-primary outline-none text-base text-slate-700 dark:text-slate-300 font-medium leading-relaxed transition-all"
                      placeholder="Enter your professional bio..."
                    />
                    <div className="flex items-center gap-3 p-4 bg-amber-50 dark:bg-amber-900/10 rounded-2xl border border-amber-100 dark:border-amber-900/50">
                      <span className="material-symbols-outlined text-amber-600 dark:text-amber-400 text-sm">info</span>
                      <p className="text-[10px] font-bold text-amber-700 dark:text-amber-300 uppercase tracking-widest">Changes are stored locally until synchronized with the secure vault.</p>
                    </div>
                  </div>
                ) : (
                  <p className="text-slate-600 dark:text-slate-300 leading-loose text-lg font-medium">
                    {profile.bio || `With over ${profile.yearsOfExperience || '0'} years of dedicated practice, ${profile.fullName} has built a reputation for excellence in his chosen fields. He has successfully represented numerous clients in landmark disputes.`}
                  </p>
                )}
              </div>
            </div>

            {/* Experience Timeline Section */}
            <div className="bg-white dark:bg-slate-900 rounded-[2rem] p-10 border border-slate-200 dark:border-slate-800 shadow-sm animate-in fade-in slide-in-from-bottom-7 duration-700 delay-150">
              <div className="flex justify-between items-center mb-8">
                <h3 className="text-xl font-black text-slate-900 dark:text-white flex items-center gap-3">
                  <span className="material-symbols-outlined text-primary">history_edu</span>
                  Experience Timeline
                </h3>
              </div>

              {isEditing ? (
                <div className="space-y-4">
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-widest">JSON Configuration (Advanced)</p>
                  <textarea
                    name="experienceTimeline"
                    value={formData.experienceTimeline || ''}
                    onChange={handleInputChange}
                    className="w-full h-48 bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700 rounded-xl p-4 font-mono text-xs text-slate-600 dark:text-slate-300 focus:border-primary outline-none resize-none"
                    placeholder='[{"year": "2020", "title": "Senior Partner", "desc": "Led the merger of..."}]'
                  />
                </div>
              ) : (
                <div className="relative border-l-2 border-slate-100 dark:border-slate-800 ml-3 space-y-8 pl-8 py-2">
                  {(profile.experienceTimeline ? JSON.parse(profile.experienceTimeline) : [
                    { year: '2023', title: 'Senior Legal Consultant', desc: 'Appointed as lead counsel for TechSlam Innovations.' },
                    { year: '2018', title: 'Partner, LegalFirm LLP', desc: 'Specialized in Corporate Law and IP disputes.' },
                    { year: '2015', title: 'Associate Attorney', desc: 'Started practice at HighCourt focusing on civil litigation.' }
                  ]).map((item, idx) => (
                    <div key={idx} className="relative group">
                      <div className="absolute -left-[41px] top-1 size-6 rounded-full bg-white dark:bg-slate-900 border-4 border-slate-200 dark:border-slate-800 group-hover:border-primary transition-colors"></div>
                      <span className="px-3 py-1 bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 rounded-lg text-[10px] font-black uppercase tracking-widest">{item.year}</span>
                      <h4 className="text-lg font-bold text-slate-900 dark:text-white mt-2">{item.title}</h4>
                      <p className="text-slate-500 dark:text-slate-400 text-sm mt-1 leading-relaxed">{item.desc}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Expertise Tags */}
            <div className="bg-white dark:bg-slate-900 rounded-[2rem] p-10 border border-slate-200 dark:border-slate-800 shadow-sm animate-in fade-in slide-in-from-bottom-8 duration-700 delay-200">
              <div className="flex justify-between items-center mb-8">
                <h3 className="text-xl font-black text-slate-900 dark:text-white flex items-center gap-3">
                  <span className="material-symbols-outlined text-primary">gavel</span>
                  Core Legal Expertise
                </h3>
                {isOwnProfile && isEditing && (
                  <div className="flex gap-2">
                    <select
                      onChange={(e) => {
                        if (e.target.value) toggleCategory(e.target.value);
                        e.target.value = '';
                      }}
                      className="text-xs font-bold text-primary bg-primary/5 border-none rounded-xl px-4 py-2 outline-none cursor-pointer"
                    >
                      <option value="">+ Add Expertise</option>
                      {categories.map(c => <option key={c} value={c}>{c}</option>)}
                    </select>
                  </div>
                )}
              </div>
              <div className="flex flex-wrap gap-4">
                {getSpecsArray(formData.specializations).map(cat => (
                  <div key={cat} className="group relative">
                    <span className={`px-5 py-3 bg-slate-50 dark:bg-slate-800 border ${isEditing ? 'border-primary/40' : 'border-slate-200'} dark:border-slate-700 rounded-2xl text-xs font-black text-slate-700 dark:text-slate-200 flex items-center gap-3 shadow-sm group-hover:border-primary/30 transition-all uppercase tracking-widest`}>
                      {cat}
                      {isOwnProfile && isEditing && (
                        <button
                          onClick={() => removeTag(cat)}
                          className="material-symbols-outlined text-base text-slate-400 hover:text-rose-500 transition-colors"
                        >
                          close
                        </button>
                      )}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            {/* Case Successes */}
            <div className="bg-white dark:bg-slate-900 rounded-[2rem] p-10 border border-slate-200 dark:border-slate-800 shadow-sm animate-in fade-in slide-in-from-bottom-10 duration-700 delay-300">
              <div className="flex justify-between items-center mb-10">
                <h3 className="text-xl font-black text-slate-900 dark:text-white flex items-center gap-3">
                  <span className="material-symbols-outlined text-primary">emoji_events</span>
                  Notable Successes
                </h3>
                {isOwnProfile && isEditing && (
                  <button
                    onClick={handleAddSuccess}
                    className="flex items-center gap-2 text-xs font-black text-white bg-primary px-6 py-3 rounded-2xl hover:opacity-95 transition-all shadow-xl shadow-primary/20 uppercase tracking-widest"
                  >
                    <span className="material-symbols-outlined text-lg">add_circle</span>
                    New Success Record
                  </button>
                )}
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                {successes.length === 0 ? (
                  <div className="col-span-full py-12 bg-slate-50/50 dark:bg-slate-800/30 rounded-3xl border border-dashed border-slate-200 dark:border-slate-700 text-center">
                    <p className="text-slate-400 font-bold italic">No notable successes recorded yet.</p>
                  </div>
                ) : (
                  successes.map(sc => (
                    <div key={sc.id} className="p-8 border border-slate-100 dark:border-slate-800 rounded-3xl bg-slate-50/50 dark:bg-slate-800/30 group relative hover:border-primary/20 transition-all">
                      <div className="flex items-start gap-5">
                        <div className="mt-1 size-10 rounded-2xl bg-primary flex items-center justify-center shrink-0 shadow-lg shadow-primary/20">
                          <span className="material-symbols-outlined text-white text-base">done</span>
                        </div>
                        <div className="flex-1 space-y-3">
                          {isEditing ? (
                            <>
                              <input
                                type="text"
                                value={sc.title}
                                onChange={(e) => handleUpdateSuccess(sc.id, 'title', e.target.value)}
                                className="w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 p-3 rounded-xl font-bold text-slate-900 dark:text-white outline-none focus:ring-2 focus:ring-primary/20"
                                placeholder="Case Title"
                              />
                              <textarea
                                value={sc.desc}
                                onChange={(e) => handleUpdateSuccess(sc.id, 'desc', e.target.value)}
                                className="w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 p-3 rounded-xl text-sm font-medium text-slate-600 dark:text-slate-400 outline-none focus:ring-2 focus:ring-primary/20 min-h-[100px]"
                                placeholder="Case Description"
                              />
                            </>
                          ) : (
                            <>
                              <h4 className="font-black text-slate-950 dark:text-white text-lg tracking-tight leading-tight">{sc.title}</h4>
                              <p className="text-sm text-slate-500 dark:text-slate-400 mt-3 leading-relaxed font-medium">{sc.desc}</p>
                            </>
                          )}
                        </div>
                      </div>
                      {isOwnProfile && (
                        <div className={`absolute top-6 right-6 flex gap-2 ${isEditing ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'} transition-all`}>
                          <button
                            onClick={() => handleDeleteSuccess(sc.id)}
                            className="size-9 bg-white dark:bg-slate-700 rounded-xl shadow-md text-slate-400 dark:text-slate-300 hover:text-rose-500 transition-colors flex items-center justify-center"
                          >
                            <span className="material-symbols-outlined text-[18px]">delete</span>
                          </button>
                        </div>
                      )}
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Testimonials Section */}
            <div className="bg-white dark:bg-slate-900 rounded-[2rem] p-10 border border-slate-200 dark:border-slate-800 shadow-sm animate-in fade-in slide-in-from-bottom-12 duration-700 delay-400">
              <div className="flex justify-between items-center mb-10">
                <h3 className="text-xl font-black text-slate-900 dark:text-white flex items-center gap-3">
                  <span className="material-symbols-outlined text-primary">reviews</span>
                  Client Testimonials
                </h3>
                <div className="px-4 py-2 bg-slate-50 dark:bg-slate-800 rounded-xl text-[10px] font-black uppercase tracking-widest text-slate-400">
                  {reviews.length} Verified Reviews
                </div>
              </div>

              {reviews.length === 0 ? (
                <div className="text-center py-12 bg-slate-50/50 dark:bg-slate-800/30 rounded-3xl border border-dashed border-slate-200 dark:border-slate-700">
                  <p className="text-slate-400 font-bold italic">No public testimonials recorded for this counsel yet.</p>
                </div>
              ) : (
                <div className="space-y-6">
                  {reviews.map(review => (
                    <div key={review.id} className="p-8 border border-slate-100 dark:border-slate-800 rounded-3xl bg-slate-50/50 dark:bg-slate-800/30 hover:shadow-lg transition-all">
                      <div className="flex justify-between items-start mb-4">
                        <div className="flex gap-1 text-amber-400">
                          {[...Array(5)].map((_, i) => (
                            <span key={i} className={`material-symbols-outlined text-sm ${i < review.rating ? 'fill-icon' : 'opacity-20'}`}>
                              star
                            </span>
                          ))}
                        </div>
                        <time className="text-[10px] font-black text-slate-400 uppercase tracking-widest">
                          {new Date(review.createdAt).toLocaleDateString()}
                        </time>
                      </div>
                      <p className="text-slate-600 dark:text-slate-300 font-medium leading-relaxed italic">
                        "{review.comment || 'Professional legal counsel provided. Highly recommended.'}"
                      </p>
                      <div className="mt-4 flex items-center gap-3">
                        <div className="w-6 h-6 rounded-full bg-slate-200 dark:bg-slate-700 flex items-center justify-center text-[8px] font-black text-slate-400">
                          ID
                        </div>
                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Counsel Client #{review.userId}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </article>

          <aside className="lg:col-span-3 space-y-8 animate-in fade-in slide-in-from-right-4 duration-700 delay-400">
            {/* Highlights Widget */}
            <div className="bg-white dark:bg-slate-900 rounded-[2rem] p-8 border border-slate-200 dark:border-slate-800 shadow-sm relative group">
              <div className="flex justify-between items-center mb-8">
                <h3 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400">Intelligence Matrix</h3>
              </div>
              <div className="space-y-8">
                <div className="flex items-center gap-4">
                  <div className="bg-blue-50 dark:bg-blue-900/20 size-12 rounded-2xl flex items-center justify-center text-primary dark:text-blue-300 shadow-sm">
                    <span className="material-symbols-outlined">work_history</span>
                  </div>
                  <div>
                    <p className="text-[10px] text-slate-400 uppercase font-black tracking-widest">Experience</p>
                    {isEditing ? (
                      <div className="flex items-center gap-4 mt-2">
                        <button
                          onClick={() => setFormData(prev => ({ ...prev, yearsOfExperience: Math.max(0, (prev.yearsOfExperience || 0) - 1) }))}
                          className="size-8 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-600 dark:text-slate-300 hover:bg-primary hover:text-white transition-all"
                        >
                          <span className="material-symbols-outlined text-sm">remove</span>
                        </button>
                        <span className="text-xl font-black text-slate-950 dark:text-white">{formData.yearsOfExperience || 0}</span>
                        <button
                          onClick={() => setFormData(prev => ({ ...prev, yearsOfExperience: (prev.yearsOfExperience || 0) + 1 }))}
                          className="size-8 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-600 dark:text-slate-300 hover:bg-primary hover:text-white transition-all"
                        >
                          <span className="material-symbols-outlined text-sm">add</span>
                        </button>
                      </div>
                    ) : (
                      <p className="text-xl font-black text-slate-950 dark:text-white leading-none mt-1">{profile.yearsOfExperience || '15'}+ Yrs</p>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <div>
                    <p className="text-[10px] text-slate-400 uppercase font-black tracking-widest">Success rate</p>
                    <p className="text-xl font-black text-slate-950 dark:text-white leading-none mt-1">{profile.completedCasesCount > 0 ? '94% Win' : 'New Counsel'}</p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <div className="bg-emerald-50 dark:bg-emerald-900/20 size-12 rounded-2xl flex items-center justify-center text-emerald-600 dark:text-emerald-400 shadow-sm">
                    <span className="material-symbols-outlined">gavel</span>
                  </div>
                  <div>
                    <p className="text-[10px] text-slate-400 uppercase font-black tracking-widest">Resolved</p>
                    <p className="text-xl font-black text-slate-950 dark:text-white leading-none mt-1">{profile.completedCasesCount || 0} Cases</p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <div className="bg-purple-50 dark:bg-purple-900/20 size-12 rounded-2xl flex items-center justify-center text-purple-600 dark:text-purple-400 shadow-sm">
                    <span className="material-symbols-outlined">translate</span>
                  </div>
                  <div>
                    <p className="text-[10px] text-slate-400 uppercase font-black tracking-widest">Channels</p>
                    {isEditing ? (
                      <div className="flex flex-wrap gap-2 mt-2">
                        {['Video', 'Audio', 'In-Person', 'Text Chat'].map(mode => (
                          <button
                            key={mode}
                            onClick={() => {
                              const current = formData.consultationModes?.split(',').map(s => s.trim()).filter(Boolean) || [];
                              const next = current.includes(mode) ? current.filter(m => m !== mode) : [...current, mode];
                              setFormData(prev => ({ ...prev, consultationModes: next.join(', ') }));
                            }}
                            className={`px-3 py-1.5 rounded-xl text-[9px] font-black uppercase tracking-widest transition-all ${(formData.consultationModes || '').includes(mode)
                              ? 'bg-purple-600 text-white shadow-lg shadow-purple-600/20'
                              : 'bg-slate-100 dark:bg-slate-800 text-slate-400 border border-slate-200 dark:border-slate-700'
                              }`}
                          >
                            {mode}
                          </button>
                        ))}
                      </div>
                    ) : (
                      <p className="text-xl font-black text-slate-950 dark:text-white leading-none mt-1">{profile.consultationModes || 'Video/Audio'}</p>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Performance Widget */}
            <div className="bg-white dark:bg-slate-900 rounded-[2rem] p-8 border border-slate-200 dark:border-slate-800 shadow-sm text-center">
              <h3 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 mb-6">Engagement Score</h3>
              <div className="flex flex-col items-center mb-8">
                <span className="text-6xl font-black text-slate-950 dark:text-white tracking-tighter">{profile.rating?.toFixed(1) || '4.9'}</span>
                <div className="flex gap-1 mt-3 text-amber-400">
                  <span className="material-symbols-outlined fill-icon text-2xl">star</span>
                  <span className="material-symbols-outlined fill-icon text-2xl">star</span>
                  <span className="material-symbols-outlined fill-icon text-2xl">star</span>
                  <span className="material-symbols-outlined fill-icon text-2xl">star</span>
                  <span className="material-symbols-outlined fill-icon text-2xl">star</span>
                </div>
                <p className="text-[10px] font-bold text-slate-400 mt-4 uppercase tracking-[0.2em]">Neural aggregation active</p>
              </div>
              <div className="p-5 bg-slate-50 dark:bg-slate-800/50 rounded-2xl border border-slate-100 dark:border-slate-800">
                <p className="text-[10px] text-slate-500 dark:text-slate-400 italic font-medium leading-relaxed uppercase tracking-wider">Historical records are permanent to maintain protocol integrity.</p>
              </div>
            </div>

            {/* Completion Checklist (Visible only to the lawyer if unverified) */}
            {isOwnProfile && !isVerified && (
              <div className="bg-amber-50/50 dark:bg-amber-900/10 rounded-[2rem] p-8 border border-amber-200/50 dark:border-amber-800/50 shadow-sm animate-in fade-in slide-in-from-bottom-4 duration-500">
                <h3 className="text-[10px] font-black uppercase tracking-[0.2em] text-amber-600 dark:text-amber-400 mb-6 flex items-center gap-2">
                  <span className="material-symbols-outlined text-sm">assignment_turned_in</span>
                  Verification Checklist
                </h3>
                <div className="space-y-4">
                  <div className="flex items-center gap-3">
                    <span className={`material-symbols-outlined text-sm ${profile.bio ? 'text-emerald-500' : 'text-amber-300'}`}>
                      {profile.bio ? 'check_circle' : 'radio_button_unchecked'}
                    </span>
                    <span className={`text-xs font-bold ${profile.bio ? 'text-slate-500 line-through' : 'text-slate-700 dark:text-slate-300'}`}>Professional Bio</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className={`material-symbols-outlined text-sm ${profile.consultationModes ? 'text-emerald-500' : 'text-amber-300'}`}>
                      {profile.consultationModes ? 'check_circle' : 'radio_button_unchecked'}
                    </span>
                    <span className={`text-xs font-bold ${profile.consultationModes ? 'text-slate-500 line-through' : 'text-slate-700 dark:text-slate-300'}`}>Consultation Modes</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className={`material-symbols-outlined text-sm ${profile.yearsOfExperience > 0 ? 'text-emerald-500' : 'text-amber-300'}`}>
                      {profile.yearsOfExperience > 0 ? 'check_circle' : 'radio_button_unchecked'}
                    </span>
                    <span className={`text-xs font-bold ${profile.yearsOfExperience > 0 ? 'text-slate-500 line-through' : 'text-slate-700 dark:text-slate-300'}`}>Years of Experience</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className={`material-symbols-outlined text-sm ${profile.specializations?.length > 0 ? 'text-emerald-500' : 'text-amber-300'}`}>
                      {profile.specializations?.length > 0 ? 'check_circle' : 'radio_button_unchecked'}
                    </span>
                    <span className={`text-xs font-bold ${profile.specializations?.length > 0 ? 'text-slate-500 line-through' : 'text-slate-700 dark:text-slate-300'}`}>Expertise Mapping</span>
                  </div>
                </div>
                <div className="mt-6 p-4 bg-white/50 dark:bg-white/5 rounded-xl text-[9px] font-bold text-amber-600 dark:text-amber-400 uppercase tracking-widest leading-relaxed">
                  Complete all fields to initiate the Neural Trust Protocol and access active cases.
                </div>
              </div>
            )}
          </aside>
        </div>
      </main>

      {/* Global Save Bar */}
      {
        isOwnProfile && hasChanges && (
          <div className="fixed bottom-0 left-0 right-0 bg-white/80 dark:bg-slate-900/80 backdrop-blur-xl border-t border-slate-200 dark:border-slate-800 py-6 px-10 z-[60] animate-in slide-in-from-bottom-10 duration-500">
            <div className="max-w-[1280px] mx-auto flex items-center justify-between">
              <div className="flex items-center gap-4">
                <div className="size-10 rounded-full bg-amber-500/10 flex items-center justify-center text-amber-500">
                  <span className="material-symbols-outlined animate-pulse">pending_actions</span>
                </div>
                <div>
                  <p className="text-sm font-black text-slate-900 dark:text-white uppercase tracking-tight">Unsaved Delta Detected</p>
                  <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-0.5">Profile modifications pending sync</p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <button
                  onClick={() => setFormData(profile)}
                  className="px-6 py-3 rounded-xl text-xs font-black text-slate-500 hover:text-slate-800 dark:hover:text-white transition-all uppercase tracking-widest"
                >
                  Revert Changes
                </button>
                <button
                  onClick={handleSave}
                  disabled={saving}
                  className="px-8 py-3.5 bg-primary text-white rounded-2xl text-xs font-black hover:shadow-2xl hover:shadow-primary/30 transition-all flex items-center gap-3 disabled:grayscale uppercase tracking-[0.2em]"
                >
                  {saving ? (
                    <>
                      <span className="material-symbols-outlined animate-spin">refresh</span>
                      Syncing...
                    </>
                  ) : (
                    <>
                      <span className="material-symbols-outlined">cloud_upload</span>
                      Synchronize
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        )
      }
    </div >
  );
};

export default LawyerProfile;
