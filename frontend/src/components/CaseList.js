import React from 'react';
import { getCategoryStyle } from './CaseCard';

const CaseList = ({ cases, onSelectCase, showAssignButton, userType, onAssign, onAccept, onDecline, onPlayAudio }) => {
    if (!cases || cases.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center p-12 text-gray-400 bg-gray-50/50 dark:bg-white/5 rounded-2xl border border-dashed border-gray-200 dark:border-gray-800">
                <span className="material-symbols-outlined text-4xl mb-2">folder_open</span>
                <p className="font-bold text-sm uppercase tracking-widest">No Intelligence Records Found</p>
            </div>
        );
    }

    const getStatusStyles = (status) => {
        const s = status?.toLowerCase() || '';
        if (s === 'draft') return 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border-slate-200 dark:border-slate-700';
        if (s === 'active' || s === 'published' || s === 'solved' || s === 'closed' || s === 'verified') {
            return 'bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800';
        }
        if (s === 'in_progress' || s === 'in-progress' || s === 'matched' || s === 'open' || s === 'new') {
            return 'bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300 border-blue-200 dark:border-blue-800';
        }
        if (s === 'under_review') {
            return 'bg-amber-100 dark:bg-amber-900/40 text-amber-700 dark:text-amber-300 border-amber-200 dark:border-amber-800';
        }
        if (s === 'payment_pending') {
            return 'bg-indigo-100 dark:bg-indigo-900/40 text-indigo-700 dark:text-indigo-300 border-indigo-200 dark:border-indigo-800';
        }
        if (s === 'pending_approval' || s === 'pending') {
            return 'bg-orange-100 dark:bg-orange-900/40 text-orange-700 dark:text-orange-300 border-orange-200 dark:border-orange-800';
        }
        return 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border-slate-200 dark:border-slate-700';
    };

    const getStatusIcon = (status) => {
        const s = status?.toLowerCase() || '';
        switch (s) {
            case 'draft': return 'draft';
            case 'published': return 'publish';
            case 'verified': return 'verified';
            case 'solved': return 'check_circle';
            case 'under_review': return 'visibility';
            case 'payment_pending': return 'payments';
            case 'pending_approval': return 'schedule';
            case 'in_progress': return 'rotate_right';
            default: return 'info';
        }
    };

    const formatCaseType = (type) => {
        if (!type) return 'General Legal';
        return type.toLowerCase().split('_')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1))
            .join(' ');
    };

    return (
        <div className="w-full overflow-hidden">
            <table className="w-full text-left border-collapse">
                <thead className="bg-slate-50 dark:bg-slate-800/50 text-slate-400 dark:text-slate-300 text-[10px] uppercase font-black tracking-[0.2em]">
                    <tr>
                        <th className="px-6 py-5">Identification</th>
                        <th className="px-6 py-5">Title & Context</th>
                        {userType === 'lawyer' && <th className="px-6 py-5">Intelligence Source</th>}
                        <th className="px-6 py-5 text-center">System Status</th>
                        <th className="px-6 py-5">Chronology</th>
                        <th className="px-6 py-5 text-right">Actions</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {cases.map((caseItem) => {
                        const style = getCategoryStyle(caseItem.caseType);
                        return (
                            <tr
                                key={caseItem.id}
                                className="group hover:bg-slate-50/50 dark:hover:bg-white/5 transition-all cursor-pointer"
                                onClick={() => onSelectCase && onSelectCase(caseItem)}
                            >
                                <td className="px-6 py-6">
                                    <div className={`flex items-center justify-center size-10 rounded-xl ${style.bg} ${style.text} font-black text-xs shadow-sm shadow-black/5 mb-[-4px] overflow-hidden relative`}>
                                        <div className={`absolute inset-0 bg-gradient-to-br ${style.gradient} opacity-30`}></div>
                                        <span className="relative z-10">#{caseItem.id}</span>
                                    </div>
                                </td>
                                <td className="px-6 py-6">
                                    <div className="flex flex-col">
                                        <div className="flex items-center gap-2">
                                            <span className={`material-symbols-outlined ${style.text} !text-lg`}>
                                                {style.icon}
                                            </span>
                                            <span className="text-sm font-black text-slate-900 dark:text-white group-hover:text-primary transition-colors tracking-tight">
                                                {caseItem.caseTitle}
                                            </span>
                                        </div>
                                        <div className="flex items-center gap-2 mt-1.5 ml-7">
                                            <span className={`text-[10px] font-black uppercase tracking-widest ${style.text}`}>
                                                {formatCaseType(caseItem.caseType)}
                                            </span>
                                        </div>
                                    </div>
                                </td>
                                {userType === 'lawyer' && (
                                    <td className="px-6 py-6">
                                        <div className="flex items-center gap-2">
                                            <div className="size-8 rounded-full bg-slate-100 dark:bg-white/5 flex items-center justify-center text-slate-400">
                                                <span className="material-symbols-outlined text-sm">person</span>
                                            </div>
                                            <span className="text-xs font-bold text-slate-700 dark:text-slate-300">
                                                {caseItem.userFullName || 'Anonymous'}
                                            </span>
                                        </div>
                                    </td>
                                )}
                                <td className="px-6 py-6 text-center">
                                    <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-black uppercase border ${getStatusStyles(caseItem.caseStatus)}`}>
                                        <span className="material-symbols-outlined !text-[14px]">{getStatusIcon(caseItem.caseStatus)}</span>
                                        {caseItem.caseStatus?.replace('_', ' ') || 'Awaiting'}
                                    </span>
                                </td>
                                <td className="px-6 py-6 text-[11px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-widest">
                                    {new Date(caseItem.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
                                </td>
                                <td className="px-6 py-6 text-right">
                                    <div className="flex justify-end gap-2" onClick={(e) => e.stopPropagation()}>
                                        {userType === 'lawyer' && onPlayAudio && (
                                            <div className="flex items-center bg-slate-100 dark:bg-white/5 rounded-lg border border-slate-200 dark:border-white/10 overflow-hidden shadow-sm">
                                                <button
                                                    onClick={() => onPlayAudio(caseItem.id, 'en')}
                                                    className="px-2 py-1.5 text-[10px] font-black hover:text-primary transition-colors border-r border-slate-200 dark:border-white/10"
                                                    title="Listen in English"
                                                >
                                                    EN
                                                </button>
                                                <button
                                                    onClick={() => onPlayAudio(caseItem.id, 'gu')}
                                                    className="px-2 py-1.5 text-[10px] font-black hover:text-primary transition-colors"
                                                    title="Listen in Gujarati"
                                                >
                                                    GU
                                                </button>
                                                <div className="px-2 py-1.5 text-slate-400 border-l border-slate-200 dark:border-white/10 flex items-center">
                                                    <span className="material-symbols-outlined !text-sm">volume_up</span>
                                                </div>
                                            </div>
                                        )}
                                        {userType === 'lawyer' && caseItem.caseStatus?.toLowerCase() === 'pending_approval' && (
                                            <>
                                                <button
                                                    onClick={() => onSelectCase && onSelectCase(caseItem)}
                                                    className="px-4 py-2 bg-emerald-500 text-white text-[10px] font-black uppercase tracking-widest rounded-lg hover:opacity-90 transition-all shadow-lg shadow-emerald-500/20 flex items-center gap-2"
                                                >
                                                    <span className="material-symbols-outlined !text-sm">gavel</span>
                                                    Review & Quote
                                                </button>
                                                <button
                                                    onClick={() => onDecline && onDecline(caseItem.id)}
                                                    className="p-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors shadow-lg shadow-red-500/20"
                                                >
                                                    <span className="material-symbols-outlined !text-lg">close</span>
                                                </button>
                                            </>
                                        )}
                                        {showAssignButton && (
                                            <button
                                                onClick={() => onAssign && onAssign(caseItem.id)}
                                                className="px-4 py-2 bg-primary text-white text-[10px] font-black uppercase tracking-widest rounded-lg hover:opacity-90 transition-all shadow-lg shadow-primary/20"
                                            >
                                                Submit Quote
                                            </button>
                                        )}
                                        {!showAssignButton && !onAccept && (
                                            <span className="material-symbols-outlined text-slate-300 group-hover:text-primary group-hover:translate-x-1 transition-all">arrow_forward</span>
                                        )}
                                    </div>
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div >
    );
};

export default CaseList;
