import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { lawyersApi, casesApi } from '../utils/api';
import { toast } from 'react-toastify';

const LawyerSearch = ({ caseId, initialSpecialization, onSelectSuccess, onBack }) => {
    const navigate = useNavigate();
    const [lawyers, setLawyers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [filters, setFilters] = useState({
        specialization: initialSpecialization || '',
        minRating: 0,
        minExperience: 0,
        name: ''
    });
    const [page, setPage] = useState(0);

    const [totalPages, setTotalPages] = useState(0);

    const fetchLawyers = useCallback(async () => {
        setLoading(true);
        try {
            const params = {
                ...filters,
                page,
                size: 10,
                sort: 'rating,desc'
            };
            // Clean up empty filters
            Object.keys(params).forEach(key => {
                if (params[key] === '' || params[key] === 0) delete params[key];
            });

            const response = await lawyersApi.search(params);
            setLawyers(response.data.lawyers || []);
            setTotalPages(response.data.totalPages || 0);
        } catch (err) {
            console.error('Error fetching lawyers:', err);
            const errorMsg = err.response?.data?.message || err.response?.data?.error || 'Failed to load lawyers. Please try again.';
            toast.error(errorMsg);
        } finally {
            setLoading(false);
        }
    }, [filters, page]);

    useEffect(() => {
        const timer = setTimeout(() => {
            fetchLawyers();
        }, 500); // Debounce search
        return () => clearTimeout(timer);
    }, [fetchLawyers]);

    const handleFilterChange = (e) => {
        const { name, value } = e.target;
        setFilters(prev => ({ ...prev, [name]: value }));
        setPage(0); // Reset to first page on filter change
    };

    const handleAssign = async (lawyerId) => {
        if (!caseId) {
            toast.error('No case selected for assignment');
            return;
        }

        try {
            await casesApi.assignLawyer(caseId, lawyerId);
            toast.success('Lawyer assigned successfully!');
            if (onSelectSuccess) onSelectSuccess(lawyerId);
        } catch (err) {
            console.error('Error assigning lawyer:', err);
            toast.error(err.response?.data?.message || 'Failed to assign lawyer.');
        }
    };

    return (
        <div className="flex flex-col lg:flex-row gap-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
            {/* Sidebar Filters */}
            <aside className="w-full lg:w-80 flex-shrink-0">
                {onBack && (
                    <button onClick={onBack} className="flex items-center gap-2 group text-gray-500 hover:text-primary dark:hover:text-white transition-colors mb-6 ml-2">
                        <span className="material-symbols-outlined transition-transform group-hover:-translate-x-1">arrow_back</span>
                        <span className="text-[10px] font-black uppercase tracking-widest">Return to Case</span>
                    </button>
                )}
                <div className="glass-card bg-white/10 p-8 rounded-[2rem] border border-white/20 shadow-2xl sticky top-8">
                    <h3 className="text-xl font-black text-primary dark:text-white mb-8 tracking-tight flex items-center gap-3">
                        <span className="material-symbols-outlined text-electric-blue">filter_list</span>
                        Filter Experts
                    </h3>

                    <div className="space-y-6">
                        <div className="flex flex-col gap-2">
                            <label className="text-[10px] font-black uppercase tracking-widest text-gray-400 dark:text-gray-300">Lawyer Name</label>
                            <div className="relative">
                                <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 !text-lg">search</span>
                                <input
                                    type="text"
                                    name="name"
                                    value={filters.name}
                                    onChange={handleFilterChange}
                                    placeholder="Search aliases..."
                                    className="w-full pl-11 pr-4 py-3 bg-white/5 dark:bg-black/20 border border-gray-200 dark:border-white/10 rounded-2xl text-sm focus:ring-2 focus:ring-electric-blue outline-none text-primary dark:text-white transition-all"
                                />
                            </div>
                        </div>

                        <div className="flex flex-col gap-2">
                            <label className="text-[10px] font-black uppercase tracking-widest text-gray-400 dark:text-gray-300">Specialization</label>
                            <select
                                name="specialization"
                                value={filters.specialization}
                                onChange={handleFilterChange}
                                className="w-full px-4 py-3 bg-white/5 dark:bg-black/20 border border-gray-200 dark:border-white/10 rounded-2xl text-sm focus:ring-2 focus:ring-electric-blue outline-none text-primary dark:text-white appearance-none transition-all cursor-pointer"
                            >
                                <option value="">All Fields</option>
                                <option value="Family Law">Family Law</option>
                                <option value="Criminal Law">Criminal Law</option>
                                <option value="Civil Law">Civil Law</option>
                                <option value="Corporate Law">Corporate Law</option>
                                <option value="Real Estate">Real Estate</option>
                            </select>
                        </div>

                        <div className="flex flex-col gap-2">
                            <label className="text-[10px] font-black uppercase tracking-widest text-gray-400 dark:text-gray-300 flex justify-between">
                                <span>Min Rating</span>
                                <span className="text-electric-blue">{filters.minRating} ★</span>
                            </label>
                            <input
                                type="range"
                                name="minRating"
                                min="0"
                                max="5"
                                step="0.5"
                                value={filters.minRating}
                                onChange={handleFilterChange}
                                className="w-full accent-electric-blue cursor-pointer"
                            />
                        </div>

                        <div className="flex flex-col gap-2">
                            <label className="text-[10px] font-black uppercase tracking-widest text-gray-400 dark:text-gray-300 flex justify-between">
                                <span>Min experience</span>
                                <span className="text-electric-blue">{filters.minExperience}+ yrs</span>
                            </label>
                            <input
                                type="range"
                                name="minExperience"
                                min="0"
                                max="30"
                                value={filters.minExperience}
                                onChange={handleFilterChange}
                                className="w-full accent-electric-blue cursor-pointer"
                            />
                        </div>


                    </div>
                </div>
            </aside>

            {/* Results Section */}
            <main className="flex-1 space-y-8">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white dark:bg-white/5 p-6 rounded-3xl border border-gray-100 dark:border-white/10">
                    <div>
                        <h2 className="text-2xl font-black text-primary dark:text-white tracking-tight">Available Legal Experts</h2>
                        <p className="text-xs font-bold text-gray-500 dark:text-gray-400 mt-1 uppercase tracking-widest">
                            Syncing {lawyers.length} expert nodes
                        </p>
                    </div>
                </div>

                {loading ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 scale-95 transition-all opacity-50">
                        {[1, 2, 3, 4].map(i => (
                            <div key={i} className="h-64 bg-gray-100 dark:bg-white/5 rounded-3xl animate-pulse"></div>
                        ))}
                    </div>
                ) : lawyers.length > 0 ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {lawyers.map(lawyer => (
                            <div key={lawyer.id} className="group glass-card bg-white dark:bg-white/5 p-8 rounded-[2rem] border border-gray-100 dark:border-white/10 shadow-xl hover-lift transition-all">
                                <div className="flex justify-between items-start mb-6">
                                    <div className="flex items-center gap-4">
                                        <div className="w-16 h-16 rounded-2xl bg-electric-blue/10 flex items-center justify-center text-electric-blue font-black text-2xl border border-electric-blue/20 group-hover:scale-110 transition-transform">
                                            {lawyer.fullName?.charAt(0) || 'E'}
                                        </div>
                                        <div>
                                            <h4 className="text-xl font-black text-primary dark:text-white">{lawyer.fullName}</h4>
                                            <span className="px-3 py-1 rounded-full bg-electric-blue/10 text-electric-blue text-[10px] font-black uppercase inline-block mt-2">
                                                {lawyer.specialization || 'General Practice'}
                                            </span>
                                        </div>
                                    </div>
                                    <div className="bg-amber-500/10 text-amber-500 px-3 py-1 rounded-lg text-[10px] font-black uppercase">
                                        {lawyer.rating || 'N/A'} ★
                                    </div>
                                </div>

                                <div className="grid grid-cols-2 gap-4 mb-8">
                                    <div className="p-4 bg-gray-50 dark:bg-black/20 rounded-2xl border border-gray-100 dark:border-white/5">
                                        <p className="text-[10px] font-black text-gray-400 dark:text-gray-500 uppercase tracking-widest mb-1">Tenure</p>
                                        <p className="font-bold text-primary dark:text-white">{lawyer.yearsOfExperience || 0} Years</p>
                                    </div>
                                    <div className="p-4 bg-gray-50 dark:bg-black/20 rounded-2xl border border-gray-100 dark:border-white/5">
                                        <p className="text-[10px] font-black text-gray-400 dark:text-gray-500 uppercase tracking-widest mb-1">Success</p>
                                        <p className="font-bold text-primary dark:text-white">{lawyer.completedCasesCount || 0} Cases</p>
                                    </div>
                                    <div className="p-4 bg-gray-50 dark:bg-black/20 rounded-2xl border border-gray-100 dark:border-white/5 col-span-2">
                                        <p className="text-[10px] font-black text-gray-400 dark:text-gray-500 uppercase tracking-widest mb-1">Languages</p>
                                        <p className="font-bold text-primary dark:text-white text-sm">{lawyer.languagesKnown || 'English'}</p>
                                    </div>
                                    {lawyer.availabilityInfo && (
                                        <div className="p-4 bg-emerald-50 dark:bg-emerald-900/10 rounded-2xl border border-emerald-100 dark:border-emerald-800/20 col-span-2 flex items-center gap-2">
                                            <span className="material-symbols-outlined text-emerald-500 text-sm">event_available</span>
                                            <p className="font-bold text-emerald-700 dark:text-emerald-400 text-xs">{lawyer.availabilityInfo}</p>
                                        </div>
                                    )}
                                </div>

                                <div className="flex gap-3">
                                    <button
                                        className="flex-1 py-4 bg-gray-100 dark:bg-white/5 text-primary dark:text-white rounded-2xl text-[10px] font-black uppercase tracking-widest hover:bg-gray-200 dark:hover:bg-white/10 transition-all border border-transparent hover:border-white/20 flex items-center justify-center gap-2"
                                        onClick={() => window.open(`${window.location.origin}/lawyer/${lawyer.id}?caseId=${caseId || ''}`, '_blank')}
                                    >
                                        <span className="material-symbols-outlined !text-lg">visibility</span>
                                        Audit Profile
                                    </button>
                                    <button
                                        className="flex-1 py-4 bg-electric-blue text-white rounded-2xl text-[10px] font-black uppercase tracking-widest shadow-lg shadow-electric-blue/30 hover:shadow-electric-blue/50 transition-all flex items-center justify-center gap-2"
                                        onClick={() => handleAssign(lawyer.id)}
                                    >
                                        <span className="material-symbols-outlined !text-lg">contact_emergency</span>
                                        Engage Expert
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="p-20 text-center bg-white dark:bg-white/5 rounded-[3rem] border border-dashed border-gray-200 dark:border-white/10">
                        <span className="material-symbols-outlined text-6xl text-gray-300 mb-6">psychology_alt</span>
                        <h3 className="text-xl font-black text-primary dark:text-white mb-2">No expert nodes detected</h3>
                        <p className="text-sm font-bold text-gray-500 dark:text-gray-400 uppercase tracking-widest">Adjust your search parameters to find counsel</p>
                    </div>
                )}

                {totalPages > 1 && (
                    <div className="flex items-center justify-center gap-4 mt-12 bg-white dark:bg-white/5 p-4 rounded-3xl w-fit mx-auto border border-gray-100 dark:border-white/10">
                        <button
                            disabled={page === 0}
                            onClick={() => setPage(page - 1)}
                            className="p-3 bg-gray-100 dark:bg-white/10 rounded-2xl text-primary dark:text-white disabled:opacity-20 transition-all"
                        >
                            <span className="material-symbols-outlined">arrow_back_ios_new</span>
                        </button>
                        <span className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-500 dark:text-gray-400">
                            Page {page + 1} of {totalPages}
                        </span>
                        <button
                            disabled={page === totalPages - 1}
                            onClick={() => setPage(page + 1)}
                            className="p-3 bg-gray-100 dark:bg-white/10 rounded-2xl text-primary dark:text-white disabled:opacity-20 transition-all"
                        >
                            <span className="material-symbols-outlined">arrow_forward_ios</span>
                        </button>
                    </div>
                )}
            </main>
        </div>
    );
};

export default LawyerSearch;
