import React, { useState, useEffect, useCallback } from 'react';
import UserCaseMessages from './UserCaseMessages';
import LawyerPreview from './LawyerPreview';
import Booking from './Booking';
import OffersList from './OffersList';
import RazorpayCheckout from './RazorpayCheckout';
import SubmitOfferForm from './SubmitOfferForm';
import { casesApi, documentsApi, timelineApi, reviewsApi } from '../utils/api';
import { toast } from 'react-toastify';
import { getToken } from '../utils/auth';
import LawyerSearch from './LawyerSearch';
import FeedbackModal from './FeedbackModal';

const CaseDetail = ({ caseId, userType, userId, onBack }) => {
    const [caseData, setCaseData] = useState(null);
    const [solution, setSolution] = useState('');
    const [loading, setLoading] = useState(true);
    const [savingSolution, setSavingSolution] = useState(false);
    const [error, setError] = useState('');
    const [documents, setDocuments] = useState([]);
    const [timeline, setTimeline] = useState([]);
    const [uploading, setUploading] = useState(false);
    const [activeTab, setActiveTab] = useState('messages'); // 'messages', 'offers', 'documents', 'timeline'
    const [showLawyerSearch, setShowLawyerSearch] = useState(false);
    const [showBooking, setShowBooking] = useState(false);
    const [showFeedbackModal, setShowFeedbackModal] = useState(false);
    const [hasRated, setHasRated] = useState(false);
    const [showOfferForm, setShowOfferForm] = useState(false);
    const [showPayment, setShowPayment] = useState(false);
    const [selectedOffer, setSelectedOffer] = useState(null);

    const fetchCaseDetails = useCallback(async () => {
        setLoading(true);
        try {
            const response = await casesApi.getById(caseId);
            if (response.data) {
                setCaseData(response.data);
                setSolution(response.data.solution || '');
                fetchDocuments();
                fetchTimeline();
                if (response.data.caseStatus?.toUpperCase() === 'CLOSED' && userType === 'user') {
                    checkIfRated(caseId);
                }
            }
        } catch (err) {
            console.error('Error fetching case details:', err);
            setError('Failed to load case details.');
        } finally {
            setLoading(false);
        }
    }, [caseId]);

    useEffect(() => {
        fetchCaseDetails();
    }, [fetchCaseDetails]);

    const fetchDocuments = async () => {
        try {
            const res = await documentsApi.getByCase(caseId);
            setDocuments(res.data || []);
        } catch (error) { console.error("Failed to load documents", error); }
    };

    const fetchTimeline = async () => {
        try {
            const res = await timelineApi.getTimeline(caseId);
            setTimeline(res.data || []);
        } catch (error) { console.error("Failed to load timeline", error); }
    };

    const handleFileUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;
        const formData = new FormData();
        formData.append('file', file);
        setUploading(true);
        try {
            await documentsApi.upload(formData, caseId);
            toast.success('Document uploaded');
            fetchDocuments();
            fetchTimeline();
        } catch (error) {
            toast.error('Upload failed');
        } finally {
            setUploading(false);
        }
    };

    const handleDownload = (doc) => {
        const token = getToken();
        fetch(`${documentsApi.download(doc.id)}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        })
            .then(response => {
                if (!response.ok) throw new Error('Download failed');
                return response.blob();
            })
            .then(blob => {
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = doc.fileName;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
            })
            .catch(err => toast.error('Download failed.'));
    };

    const handleAccept = async () => {
        try {
            await casesApi.accept(caseId);
            toast.success('Case accepted');
            fetchCaseDetails();
        } catch (err) { toast.error('Failed to accept'); }
    };

    const handleDecline = async () => {
        try {
            await casesApi.decline(caseId);
            toast.success('Case declined');
            onBack();
        } catch (err) { toast.error('Failed to decline'); }
    };

    const handleUpdateAdvice = async () => {
        if (!solution.trim()) return toast.warning('Advice text required');
        setSavingSolution(true);
        try {
            await casesApi.updateSolution(caseId, solution);
            toast.success('Advice saved successfully');
            fetchCaseDetails();
        } catch (err) { toast.error('Failed to save advice'); }
        finally { setSavingSolution(false); }
    };

    const handleFinalizeCase = async () => {
        if (!solution.trim()) return toast.warning('Final solution required');
        setSavingSolution(true);
        try {
            await casesApi.updateSolution(caseId, solution);
            await casesApi.updateStatus(caseId, 'CLOSED');
            toast.success('Case resolved and archived');
            fetchCaseDetails();
        } catch (err) { toast.error('Failed to resolve case'); }
        finally { setSavingSolution(false); }
    };

    const handleReopenCase = async () => {
        try {
            await casesApi.updateStatus(caseId, 'IN_PROGRESS');
            toast.success('Case reopened for further resolution');
            fetchCaseDetails();
        } catch (err) { toast.error('Failed to reopen case'); }
    };

    const checkIfRated = async (id) => {
        try {
            // Check if user has already rated this lawyer for this case
            // Simplified: Fetch reviews for lawyer and see if any match caseId and userId
            const res = await reviewsApi.getByLawyer(caseData.lawyerId);
            const reviewed = res.data.some(r => r.caseId === id && r.userId === userId);
            setHasRated(reviewed);
            if (!reviewed) setShowFeedbackModal(true);
        } catch (err) { console.error("Error checking ratings", err); }
    };

    if (loading) return (
        <div className="flex flex-col items-center justify-center h-64 animate-pulse">
            <div className="w-12 h-12 bg-gray-200 dark:bg-gray-800 rounded-full mb-4"></div>
            <div className="h-4 w-48 bg-gray-200 dark:bg-gray-800 rounded"></div>
        </div>
    );

    if (error) return (
        <div className="p-12 text-center bg-red-50 dark:bg-red-900/10 rounded-3xl border border-red-200 dark:border-red-800">
            <p className="text-red-500 font-bold mb-4">{error}</p>
            <button onClick={onBack} className="px-6 py-2 bg-red-500 text-white rounded-xl font-bold">Return to Registry</button>
        </div>
    );

    if (showLawyerSearch) {
        return (
            <LawyerSearch
                caseId={caseId}
                initialSpecialization={caseData.caseType}
                onSelectSuccess={() => { setShowLawyerSearch(false); fetchCaseDetails(); }}
                onBack={() => setShowLawyerSearch(false)}
            />
        );
    }

    if (showBooking) {
        return (
            <div className="space-y-6">
                <button onClick={() => setShowBooking(false)} className="flex items-center gap-2 text-gray-400 hover:text-primary transition-colors">
                    <span className="material-symbols-outlined !text-lg">arrow_back</span>
                    <span className="text-[10px] font-black uppercase tracking-widest">Back to Case</span>
                </button>
                <Booking
                    userId={userId}
                    userType={userType}
                    preSelectedLawyerId={caseData.lawyerId}
                    preSelectedLawyerName={caseData.lawyerFullName}
                    preSelectedCaseId={caseId}
                    onBookingSuccess={() => { setShowBooking(false); fetchCaseDetails(); }}
                />
            </div>
        );
    }

    const isLawyer = userType === 'lawyer';
    const status = caseData.caseStatus?.toUpperCase();

    return (
        <div className="max-w-6xl mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
            {/* Navigation Header */}
            <div className="flex items-center justify-between">
                <button onClick={onBack} className="flex items-center gap-2 group text-gray-500 hover:text-primary dark:hover:text-white transition-colors">
                    <span className="material-symbols-outlined transition-transform group-hover:-translate-x-1">arrow_back</span>
                    <span className="text-[10px] font-black uppercase tracking-widest">Back to Registry</span>
                </button>
                <div className="flex items-center gap-3">
                    <span className={`px-4 py-1 rounded-full text-[10px] font-black uppercase border tracking-widest shadow-sm ${status === 'CLOSED' ? 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20' :
                        status === 'PENDING_APPROVAL' ? 'bg-amber-500/10 text-amber-500 border-amber-500/20' :
                            'bg-blue-500/10 text-blue-500 border-blue-500/20'
                        }`}>
                        {status?.replace('_', ' ') || 'OPEN'}
                    </span>
                    <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest bg-gray-100 dark:bg-white/5 px-2 py-1 rounded">
                        INV-{caseData.id}
                    </span>
                </div>
            </div>

            {/* Hero Summary Card */}
            <div className="bg-primary dark:bg-primary-dark p-8 rounded-[2rem] text-white shadow-2xl relative overflow-hidden">
                <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full -mr-20 -mt-20 blur-3xl"></div>
                <div className="relative z-10">
                    <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
                        <div className="flex-1 space-y-4">
                            <h1 className="text-3xl md:text-4xl font-black tracking-tight leading-tight">{caseData.caseTitle}</h1>
                            <p className="text-white/70 max-w-2xl text-lg leading-relaxed">{caseData.description}</p>
                        </div>
                        <div className="flex flex-col gap-4 min-w-[200px]">
                            {!isLawyer && !caseData.lawyerId && (
                                <button onClick={() => setShowLawyerSearch(true)} className="w-full py-4 bg-white text-primary rounded-2xl font-black uppercase tracking-widest text-[10px] hover:bg-blue-50 transition-colors shadow-lg">
                                    FIND LAWYER
                                </button>
                            )}
                            <div className="bg-white/10 backdrop-blur-md rounded-2xl p-4 border border-white/20">
                                <p className="text-[10px] font-black uppercase text-white/50 tracking-widest mb-1">Created</p>
                                <p className="font-bold text-sm">{new Date(caseData.createdAt).toLocaleDateString()}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Case Actions for Approval */}
            {status === 'PENDING_APPROVAL' && isLawyer && (
                <div className="flex items-center justify-between p-6 bg-amber-500/10 border border-amber-500/20 rounded-3xl animate-pulse">
                    <div className="flex items-center gap-4 text-amber-500">
                        <span className="material-symbols-outlined text-3xl">priority_high</span>
                        <div>
                            <p className="font-black uppercase text-xs tracking-widest">Awaiting Your Authorization</p>
                            <p className="text-xs font-bold opacity-80">Please review the evidence and accept or decline this case.</p>
                        </div>
                    </div>
                    <div className="flex gap-3">
                        <button onClick={handleAccept} className="px-6 py-2 bg-emerald-500 text-white rounded-xl text-[10px] font-black uppercase tracking-widest shadow-lg shadow-emerald-500/20">Accept</button>
                        <button onClick={handleDecline} className="px-6 py-2 bg-red-500 text-white rounded-xl text-[10px] font-black uppercase tracking-widest shadow-lg shadow-red-500/20">Decline</button>
                    </div>
                </div>
            )}

            {/* Tabbed Content Area */}
            <div className="space-y-6">
                <div className="flex gap-2 p-1.5 bg-gray-100 dark:bg-white/5 w-fit rounded-2xl border border-gray-200 dark:border-white/10">
                    {['messages', 'offers', 'documents', 'timeline'].map(tab => (
                        <button
                            key={tab}
                            onClick={() => setActiveTab(tab)}
                            className={`px-6 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all ${activeTab === tab ? 'bg-primary text-white shadow-xl translate-y-[-1px]' : 'text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300'
                                }`}
                        >
                            {tab}
                        </button>
                    ))}
                </div>

                <div className="min-h-[500px]">
                    {activeTab === 'messages' && (
                        <div className="grid grid-cols-12 gap-8">
                            <div className="col-span-12 lg:col-span-8 space-y-8">
                                {/* Messaging Component */}
                                <div className="bg-white dark:bg-background-dark/50 rounded-[2rem] border border-gray-100 dark:border-gray-800 p-8 shadow-sm">
                                    <UserCaseMessages
                                        caseId={caseId}
                                        userId={userId}
                                        userType={userType}
                                        lawyerId={caseData.lawyerId}
                                        clientUserId={caseData.userId}
                                        caseStatus={caseData.caseStatus}
                                        onCaseUpdate={(updated) => { setCaseData(updated); if (updated.solution) setSolution(updated.solution); fetchTimeline(); }}
                                    />
                                </div>
                            </div>
                            <div className="col-span-12 lg:col-span-4 space-y-8">
                                {/* Solution Box */}
                                <div className="glass-card bg-emerald-500/5 dark:bg-emerald-500/10 p-8 rounded-[2rem] border border-emerald-500/20 shadow-xl">
                                    <div className="flex items-center gap-3 mb-6">
                                        <div className="w-10 h-10 bg-emerald-500 rounded-xl flex items-center justify-center text-white shadow-lg shadow-emerald-500/20">
                                            <span className="material-symbols-outlined">gavel</span>
                                        </div>
                                        <h3 className="text-lg font-black tracking-tight text-emerald-600 dark:text-emerald-400">Legal Resolution</h3>
                                    </div>
                                    {isLawyer && status !== 'CLOSED' ? (
                                        <div className="space-y-4">
                                            <textarea
                                                value={solution}
                                                onChange={(e) => setSolution(e.target.value)}
                                                className="w-full h-48 p-4 bg-white dark:bg-gray-900 rounded-2xl border border-emerald-500/20 text-sm focus:ring-2 focus:ring-emerald-500 outline-none"
                                                placeholder="Provide your professional findings..."
                                            />
                                            <div className="flex flex-col gap-3">
                                                <button
                                                    onClick={handleUpdateAdvice}
                                                    disabled={savingSolution}
                                                    className="w-full py-4 bg-gray-100 dark:bg-white/5 text-emerald-600 dark:text-emerald-400 rounded-2xl text-[10px] font-black uppercase tracking-widest hover:bg-emerald-50 transition-all border border-emerald-500/10"
                                                >
                                                    {savingSolution ? 'Saving...' : 'Save Legal Advice'}
                                                </button>
                                            </div>
                                        </div>
                                    ) : (
                                        <div className="space-y-6">
                                            <div className="prose dark:prose-invert text-sm font-medium leading-relaxed">
                                                {solution || "No resolution has been archived yet. Pending expert review."}
                                            </div>
                                            {!isLawyer && status === 'CLOSED' && (
                                                <button
                                                    onClick={handleReopenCase}
                                                    className="w-full py-3 bg-amber-500/10 text-amber-600 dark:text-amber-400 rounded-xl text-[10px] font-black uppercase tracking-widest hover:bg-amber-500 hover:text-white transition-all border border-amber-500/20 flex items-center justify-center gap-2"
                                                >
                                                    <span className="material-symbols-outlined text-sm">refresh</span>
                                                    Not Satisfied? Reopen Case
                                                </button>
                                            )}
                                        </div>
                                    )}
                                </div>

                                {/* Intelligence Source (Client Info for Lawyer) */}
                                {isLawyer && (
                                    <div className="bg-primary/5 dark:bg-white/5 p-8 rounded-[2rem] border border-primary/20 shadow-sm relative overflow-hidden group">
                                        <div className="absolute top-0 right-0 w-32 h-32 bg-primary/10 rounded-full -mr-16 -mt-16 blur-2xl group-hover:bg-primary/20 transition-all"></div>
                                        <div className="relative z-10">
                                            <div className="flex items-center gap-3 mb-6">
                                                <div className="w-10 h-10 bg-primary text-white rounded-xl flex items-center justify-center shadow-lg shadow-primary/20">
                                                    <span className="material-symbols-outlined">person</span>
                                                </div>
                                                <div>
                                                    <h3 className="text-lg font-black tracking-tight text-primary dark:text-white">Client</h3>
                                                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-[0.15em] mt-0.5">Primary Contact Data</p>
                                                </div>
                                            </div>
                                            <div className="space-y-4">
                                                <div className="p-4 bg-white dark:bg-gray-900 rounded-2xl border border-slate-100 dark:border-white/5">
                                                    <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest mb-1">Full Name</p>
                                                    <p className="text-sm font-black text-slate-900 dark:text-white">{caseData.userFullName || 'Restricted Access'}</p>
                                                </div>
                                                <div className="p-4 bg-white dark:bg-gray-900 rounded-2xl border border-slate-100 dark:border-white/5">
                                                    <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest mb-1">Electronic Mail</p>
                                                    <p className="text-sm font-bold text-primary dark:text-blue-400 truncate">{caseData.userEmail || 'email@verification.pending'}</p>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                )}
                                {isLawyer && status !== 'CLOSED' && (
                                    <div className="bg-white dark:bg-slate-900 rounded-[2rem] p-10 border border-slate-200 dark:border-slate-800 shadow-sm animate-in fade-in slide-in-from-bottom-4">
                                        <div className="flex items-center gap-3 mb-6">
                                            <div className="size-10 rounded-xl bg-slate-100 dark:bg-white/5 flex items-center justify-center text-slate-500">
                                                <span className="material-symbols-outlined text-lg">archive</span>
                                            </div>
                                            <div>
                                                <h3 className="text-lg font-black tracking-tight text-slate-900 dark:text-white">Case Disposition</h3>
                                                <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mt-0.5">Terminal Protocol: This will archive the case and lock all edits.</p>
                                            </div>
                                        </div>
                                        <button
                                            onClick={handleFinalizeCase}
                                            disabled={savingSolution}
                                            className="w-full py-5 bg-primary text-white rounded-2xl text-[10px] font-black uppercase tracking-[0.2em] shadow-xl shadow-primary/20 hover:scale-[1.05] transition-all"
                                        >
                                            {savingSolution ? 'Finalizing...' : 'Resolve & Close Case'}
                                        </button>
                                    </div>
                                )}

                                {/* Lawyer Profile if client view */}
                                {!isLawyer && caseData.lawyerId && status !== 'PENDING_APPROVAL' && (
                                    <div className="bg-white dark:bg-background-dark/50 p-8 rounded-[2rem] border border-gray-100 dark:border-gray-800 shadow-sm">
                                        <div className="flex items-center justify-between mb-6">
                                            <h3 className="text-lg font-black tracking-tight text-primary dark:text-white">Active Counsel</h3>
                                            <span className="material-symbols-outlined text-emerald-500 fill-icon text-xl">verified</span>
                                        </div>

                                        <div className="space-y-6">
                                            <LawyerPreview lawyerId={caseData.lawyerId} />

                                            <div className="pt-6 border-t border-gray-100 dark:border-gray-800 space-y-3">
                                                {status === 'CLOSED' ? (
                                                    hasRated ? (
                                                        <div className="w-full py-4 bg-emerald-500/10 text-emerald-500 rounded-2xl text-[10px] font-black uppercase tracking-widest flex items-center justify-center gap-2 border border-emerald-500/20">
                                                            <span className="material-symbols-outlined !text-lg">verified</span>
                                                            Review Submitted
                                                        </div>
                                                    ) : (
                                                        <button
                                                            onClick={() => setShowFeedbackModal(true)}
                                                            className="w-full py-4 bg-emerald-500 text-white rounded-2xl text-[10px] font-black uppercase tracking-widest shadow-xl shadow-emerald-500/20 hover:scale-[1.02] transition-all flex items-center justify-center gap-2"
                                                        >
                                                            <span className="material-symbols-outlined !text-lg">grade</span>
                                                            Rate Experience
                                                        </button>
                                                    )
                                                ) : (
                                                    <button
                                                        onClick={() => setShowBooking(true)}
                                                        className="w-full py-4 bg-primary text-white rounded-2xl text-[10px] font-black uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-[1.02] transition-all flex items-center justify-center gap-2"
                                                    >
                                                        <span className="material-symbols-outlined !text-lg">calendar_month</span>
                                                        Book Appointment
                                                    </button>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    {activeTab === 'offers' && (
                        <div className="space-y-6">
                            {!isLawyer ? (
                                <>
                                    <OffersList
                                        caseId={caseId}
                                        caseStatus={caseData.caseStatus}
                                        onOfferAccepted={(offerId) => {
                                            setSelectedOffer(offerId);
                                            setShowPayment(true);
                                            fetchCaseDetails();
                                        }}
                                    />

                                    {showPayment && caseData.caseStatus === 'PAYMENT_PENDING' && (
                                        <RazorpayCheckout
                                            offerId={selectedOffer || caseData.selectedOfferId}
                                            caseId={caseId}
                                            onSuccess={() => {
                                                toast.success('Payment successful!');
                                                setShowPayment(false);
                                                fetchCaseDetails();
                                            }}
                                            onFailure={(error) => {
                                                toast.error(`Payment failed: ${error}`);
                                            }}
                                        />
                                    )}
                                </>
                            ) : (
                                <>
                                    {(caseData.caseStatus === 'PUBLISHED' || caseData.caseStatus === 'UNDER_REVIEW') && !showOfferForm && (
                                        <div className="text-center py-12 bg-white dark:bg-background-dark/50 rounded-[2rem] border border-gray-100 dark:border-gray-800">
                                            <div className="max-w-md mx-auto space-y-4">
                                                <div className="w-16 h-16 bg-primary/10 rounded-2xl flex items-center justify-center mx-auto">
                                                    <span className="material-symbols-outlined text-3xl text-primary">gavel</span>
                                                </div>
                                                <h3 className="text-xl font-black text-primary dark:text-white">Submit Your Proposal</h3>
                                                <p className="text-sm text-gray-600 dark:text-gray-400">Review the case details and submit your professional proposal to the client.</p>
                                                <button
                                                    onClick={() => setShowOfferForm(true)}
                                                    className="px-8 py-4 bg-primary text-white rounded-2xl font-black uppercase tracking-widest text-[10px] shadow-xl shadow-primary/20 hover:scale-105 transition-all"
                                                >
                                                    Create Proposal
                                                </button>
                                            </div>
                                        </div>
                                    )}

                                    {showOfferForm && (
                                        <SubmitOfferForm
                                            caseId={caseId}
                                            onOfferSubmitted={() => {
                                                setShowOfferForm(false);
                                                toast.success('Offer submitted successfully!');
                                                fetchCaseDetails();
                                            }}
                                            onCancel={() => setShowOfferForm(false)}
                                        />
                                    )}

                                    {caseData.caseStatus !== 'PUBLISHED' && caseData.caseStatus !== 'UNDER_REVIEW' && !showOfferForm && (
                                        <div className="text-center py-12 bg-gray-50 dark:bg-white/5 rounded-[2rem] border border-gray-100 dark:border-gray-800">
                                            <span className="material-symbols-outlined text-4xl text-gray-300 mb-4">lock</span>
                                            <p className="text-sm font-bold text-gray-400 uppercase tracking-widest">Case is no longer accepting offers</p>
                                        </div>
                                    )}
                                </>
                            )}
                        </div>
                    )}

                    {activeTab === 'documents' && (
                        <div className="bg-white dark:bg-background-dark/50 rounded-[2rem] border border-gray-100 dark:border-gray-800 p-8 shadow-sm">
                            <div className="flex justify-between items-center mb-8">
                                <h3 className="text-xl font-black tracking-tight text-primary dark:text-white">Intelligence Vault</h3>
                                <label className="flex items-center gap-2 px-6 py-3 bg-primary text-white rounded-2xl text-[10px] font-black uppercase tracking-widest cursor-pointer hover:bg-blue-600 transition-colors shadow-lg">
                                    <span className="material-symbols-outlined !text-lg">{uploading ? 'sync' : 'upload_file'}</span>
                                    {uploading ? 'Synching...' : 'Inject Evidence'}
                                    <input type="file" onChange={handleFileUpload} className="hidden" disabled={uploading} />
                                </label>
                            </div>

                            {documents.length === 0 ? (
                                <div className="text-center py-24 bg-gray-50 dark:bg-white/5 rounded-3xl border border-dashed border-gray-200 dark:border-gray-800">
                                    <span className="material-symbols-outlined text-4xl text-gray-300 mb-2">cloud_off</span>
                                    <p className="text-xs font-black text-gray-400 tracking-widest uppercase">No files decrypted in this workspace.</p>
                                </div>
                            ) : (
                                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                    {documents.map(doc => (
                                        <div key={doc.id} className="group p-6 bg-gray-50 dark:bg-gray-900 border border-transparent hover:border-blue-500/20 rounded-3xl transition-all hover:shadow-xl hover:shadow-blue-500/5">
                                            <div className="flex justify-between items-start mb-4">
                                                <div className="w-12 h-12 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 flex items-center justify-center text-blue-500 shadow-sm group-hover:scale-110 transition-transform">
                                                    <span className="material-symbols-outlined">description</span>
                                                </div>
                                                <button onClick={() => handleDownload(doc)} className="p-2 text-gray-400 hover:text-primary transition-colors">
                                                    <span className="material-symbols-outlined">download</span>
                                                </button>
                                            </div>
                                            <h4 className="font-bold text-sm mb-1 truncate text-primary dark:text-white">{doc.fileName}</h4>
                                            <p className="text-[10px] font-black text-gray-400 dark:text-gray-300 uppercase tracking-widest mb-4">
                                                {doc.mimeType?.split('/')[1] || 'DOC'} • {(doc.fileSize / 1024).toFixed(1)} KB
                                            </p>
                                            <div className="flex items-center gap-2 pt-4 border-t border-gray-100 dark:border-gray-800">
                                                <div className="w-5 h-5 bg-blue-500/10 rounded-full flex items-center justify-center text-[8px] font-black text-blue-500 uppercase">
                                                    {doc.uploadedByRole?.[0] || 'S'}
                                                </div>
                                                <span className="text-[10px] font-bold text-gray-500 dark:text-gray-400 uppercase">{doc.uploadedByRole}</span>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    {activeTab === 'timeline' && (
                        <div className="bg-white dark:bg-background-dark/50 rounded-[2rem] border border-gray-100 dark:border-gray-800 p-8 shadow-sm overflow-hidden">
                            <h3 className="text-xl font-black tracking-tight mb-12 text-primary dark:text-white">Temporal Audit Trail</h3>
                            <div className="relative space-y-12 before:absolute before:left-6 before:top-2 before:bottom-2 before:w-[2px] before:bg-gray-100 dark:before:bg-gray-800">
                                {timeline.map((item) => (
                                    <div key={item.id} className="relative pl-16 animate-in slide-in-from-left-4 duration-500">
                                        <div className={`absolute left-0 top-0 w-12 h-12 rounded-2xl flex items-center justify-center text-white z-10 shadow-lg ${item.eventType === 'STATUS_CHANGE' ? 'bg-emerald-500 shadow-emerald-500/20' :
                                            item.eventType === 'LAWYER_ASSIGNED' ? 'bg-blue-500 shadow-blue-500/20' : 'bg-primary'
                                            }`}>
                                            <span className="material-symbols-outlined !text-xl">
                                                {item.eventType === 'STATUS_CHANGE' ? 'sync_alt' :
                                                    item.eventType === 'LAWYER_ASSIGNED' ? 'person_add' : 'info'}
                                            </span>
                                        </div>
                                        <div className="glass-card bg-gray-50/50 dark:bg-white/5 p-6 rounded-3xl border border-gray-100 dark:border-gray-800 group hover:border-blue-500/30 transition-colors">
                                            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-4">
                                                <span className="px-3 py-1 bg-white dark:bg-gray-800 rounded-full text-[10px] font-black uppercase tracking-widest border border-gray-100 dark:border-gray-700">
                                                    {item.eventType.replace('_', ' ')}
                                                </span>
                                                <time className="text-[10px] font-black text-gray-400 dark:text-gray-300 uppercase tracking-widest">
                                                    {new Date(item.createdAt).toLocaleDateString()} @ {new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                                </time>
                                            </div>
                                            <p className="text-sm font-bold text-gray-700 dark:text-gray-200 mb-4">{item.message}</p>
                                            {item.oldStatus && (
                                                <div className="flex items-center gap-3 p-3 bg-white dark:bg-white/5 rounded-xl border border-gray-100 dark:border-gray-800">
                                                    <span className="text-[10px] font-black text-gray-400 dark:text-gray-300 uppercase tracking-widest line-through">{item.oldStatus}</span>
                                                    <span className="material-symbols-outlined text-gray-300 dark:text-gray-600">arrow_forward</span>
                                                    <span className="text-[10px] font-black text-emerald-500 uppercase tracking-widest">{item.newStatus}</span>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {showFeedbackModal && !hasRated && (
                <FeedbackModal
                    caseId={caseId}
                    lawyerId={caseData.lawyerId}
                    userId={userId}
                    onClose={() => setShowFeedbackModal(false)}
                    onSubmitSuccess={() => setHasRated(true)}
                />
            )}
        </div>
    );
};

export default CaseDetail;
