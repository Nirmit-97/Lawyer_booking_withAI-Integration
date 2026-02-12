import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { lawyersApi } from '../utils/api';

const LawyerPreview = ({ lawyerId }) => {
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                setLoading(true);
                const response = await lawyersApi.getProfile(lawyerId);
                setProfile(response.data);
            } catch (err) {
                console.error("Failed to load lawyer preview", err);
            } finally {
                setLoading(false);
            }
        };
        if (lawyerId) fetchProfile();
    }, [lawyerId]);

    if (loading) return (
        <div className="flex items-center gap-4 animate-pulse p-4">
            <div className="w-16 h-16 bg-slate-100 dark:bg-slate-800 rounded-2xl"></div>
            <div className="space-y-2">
                <div className="h-4 w-32 bg-slate-100 dark:bg-slate-800 rounded"></div>
                <div className="h-3 w-20 bg-slate-100 dark:bg-slate-800 rounded"></div>
            </div>
        </div>
    );

    if (!profile) return null;

    return (
        <div className="space-y-6">
            <div className="flex items-center gap-4">
                <div className="w-20 h-20 rounded-2xl bg-primary/5 dark:bg-white/5 border border-primary/10 dark:border-white/10 flex items-center justify-center text-primary dark:text-blue-400 font-black text-2xl shadow-inner overflow-hidden">
                    {profile.profilePhotoUrl ? (
                        <img src={profile.profilePhotoUrl} alt={profile.fullName} className="w-full h-full object-cover" />
                    ) : (
                        profile.fullName?.charAt(0) || 'L'
                    )}
                </div>
                <div className="flex-1 min-w-0">
                    <h4 className="text-xl font-black text-slate-900 dark:text-white truncate tracking-tight">{profile.fullName}</h4>
                    <span className="inline-block px-3 py-1 bg-primary/5 dark:bg-blue-900/20 text-primary dark:text-blue-400 text-[10px] font-black uppercase tracking-widest rounded-lg mt-1 border border-primary/10 dark:border-blue-800/50">
                        {profile.specialization || 'Legal Specialist'}
                    </span>
                </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
                <div className="p-4 bg-slate-50 dark:bg-slate-900/50 rounded-2xl border border-slate-100 dark:border-slate-800">
                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Tenure</p>
                    <p className="font-black text-slate-900 dark:text-white">{profile.yearsOfExperience || 0}+ Yrs</p>
                </div>
                <div className="p-4 bg-slate-50 dark:bg-slate-900/50 rounded-2xl border border-slate-100 dark:border-slate-800">
                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Rating</p>
                    <div className="flex items-center gap-1">
                        <span className="font-black text-slate-900 dark:text-white">{profile.rating?.toFixed(1) || 'N/A'}</span>
                        <span className="material-symbols-outlined text-sm text-amber-500 fill-icon">star</span>
                    </div>
                </div>
                <div className="p-4 bg-slate-50 dark:bg-slate-900/50 rounded-2xl border border-slate-100 dark:border-slate-800 col-span-2">
                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Languages</p>
                    <p className="font-bold text-slate-900 dark:text-white text-sm">{profile.languagesKnown || 'English'}</p>
                </div>
                {profile.availabilityInfo && (
                    <div className="p-4 bg-emerald-50 dark:bg-emerald-900/10 rounded-2xl border border-emerald-100 dark:border-emerald-800/20 col-span-2 flex items-center gap-2">
                        <span className="material-symbols-outlined text-emerald-500 text-sm">event_available</span>
                        <p className="font-bold text-emerald-700 dark:text-emerald-400 text-xs">{profile.availabilityInfo}</p>
                    </div>
                )}
            </div>

            <button
                onClick={() => navigate(`/lawyer/${lawyerId}`)}
                className="w-full py-4 bg-white dark:bg-slate-800 text-primary dark:text-white border border-slate-200 dark:border-slate-700 rounded-2xl text-[10px] font-black uppercase tracking-[0.2em] shadow-sm hover:shadow-xl hover:bg-primary hover:text-white hover:border-primary transition-all flex items-center justify-center gap-2 group"
            >
                View Professional Profile
                <span className="material-symbols-outlined text-base group-hover:translate-x-1 transition-transform">arrow_forward</span>
            </button>
        </div>
    );
};

export default LawyerPreview;
