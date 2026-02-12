import React from 'react';

/**
 * Utility Helpers for Dashboard UI Consistent Standards
 */
export const getCategoryStyle = (type) => {
    const cat = type?.toLowerCase() || '';
    if (cat.includes('criminal')) return {
        icon: 'gavel',
        bg: 'bg-red-50 dark:bg-red-900/20',
        text: 'text-red-600 dark:text-red-400',
        border: 'border-red-100 dark:border-red-900/30',
        gradient: 'from-red-500/5 to-transparent'
    };
    if (cat.includes('family')) return {
        icon: 'family_restroom',
        bg: 'bg-purple-50 dark:bg-purple-900/20',
        text: 'text-purple-600 dark:text-purple-400',
        border: 'border-purple-100 dark:border-purple-900/30',
        gradient: 'from-purple-500/5 to-transparent'
    };
    if (cat.includes('property') || cat.includes('estate') || cat.includes('land')) return {
        icon: 'home_work',
        bg: 'bg-emerald-50 dark:bg-emerald-900/20',
        text: 'text-emerald-600 dark:text-emerald-400',
        border: 'border-emerald-100 dark:border-emerald-900/30',
        gradient: 'from-emerald-500/5 to-transparent'
    };
    if (cat.includes('corporate') || cat.includes('business')) return {
        icon: 'business_center',
        bg: 'bg-blue-50 dark:bg-blue-900/20',
        text: 'text-blue-600 dark:text-blue-400',
        border: 'border-blue-100 dark:border-blue-900/30',
        gradient: 'from-blue-500/5 to-transparent'
    };
    // Default fallback
    return {
        icon: 'balance',
        bg: 'bg-slate-50 dark:bg-slate-800',
        text: 'text-slate-600 dark:text-slate-400',
        border: 'border-slate-200 dark:border-slate-700',
        gradient: 'from-slate-500/5 to-transparent'
    };
};

const CaseCard = ({ caseItem, onClick }) => {
    const style = getCategoryStyle(caseItem.caseType);

    const formatCaseType = (type) => {
        if (!type) return 'General Legal';
        return type.toLowerCase().split('_')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1))
            .join(' ');
    };

    return (
        <div
            onClick={() => onClick && onClick(caseItem)}
            className={`relative p-6 rounded-3xl border ${style.border} bg-white dark:bg-gray-900 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300 cursor-pointer group overflow-hidden`}
        >
            {/* Decorative Gradient */}
            <div className={`absolute top-0 right-0 w-32 h-32 bg-gradient-to-br ${style.gradient} rounded-bl-full opacity-50`}></div>

            <div className="flex justify-between items-start mb-4">
                <div className={`size-12 rounded-2xl flex items-center justify-center ${style.bg} ${style.text}`}>
                    <span className="material-symbols-outlined text-2xl">
                        {style.icon}
                    </span>
                </div>
                <span className={`px-3 py-1 text-[10px] font-black rounded-full uppercase tracking-wider border ${caseItem.caseStatus?.toLowerCase() === 'active' || caseItem.caseStatus?.toLowerCase() === 'published' || caseItem.caseStatus?.toLowerCase() === 'solved'
                    ? 'bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800'
                    : 'bg-orange-100 dark:bg-orange-900/40 text-orange-700 dark:text-orange-300 border-orange-200 dark:border-orange-800'
                    }`}>
                    {caseItem.caseStatus || 'Awaiting'}
                </span>
            </div>

            <div className="relative z-10">
                <h4 className="font-bold text-lg text-slate-900 dark:text-white mb-2 line-clamp-1 group-hover:text-primary transition-colors">
                    {caseItem.caseTitle}
                </h4>

                <div className="flex items-center gap-3 mb-6">
                    <span className={`text-[10px] font-black uppercase tracking-widest ${style.text}`}>
                        {formatCaseType(caseItem.caseType)}
                    </span>
                    <span className="size-1 rounded-full bg-slate-300"></span>
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">
                        {new Date(caseItem.createdAt).toLocaleDateString()}
                    </span>
                </div>

                <div className="flex items-center text-primary dark:text-white text-xs font-bold uppercase tracking-widest gap-2 opacity-0 -translate-x-2 group-hover:opacity-100 group-hover:translate-x-0 transition-all duration-300">
                    View Details <span className="material-symbols-outlined text-sm">arrow_forward</span>
                </div>
            </div>
        </div>
    );
};

export default CaseCard;
