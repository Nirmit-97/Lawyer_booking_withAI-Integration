import React, { useState, useEffect } from 'react';
import { casesApi, ttsApi } from '../utils/api';
import { toast } from 'react-toastify';

const CaseDraftPreview = ({ caseId, onPublish, onCancel }) => {
    const [caseData, setCaseData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [publishing, setPublishing] = useState(false);
    const [editing, setEditing] = useState(false);
    const [formData, setFormData] = useState({
        caseTitle: '',
        caseType: '',
        description: ''
    });
    const [audioUrl, setAudioUrl] = useState({}); // { en: url, gu: url }
    const [audioLanguage, setAudioLanguage] = useState('en');
    const [audioLoading, setAudioLoading] = useState(false);
    const [audioError, setAudioError] = useState(null);

    const caseTypes = [
        { value: 'CRIMINAL', label: 'Criminal Law' },
        { value: 'FAMILY', label: 'Family Law' },
        { value: 'PROPERTY', label: 'Property Law' },
        { value: 'CORPORATE', label: 'Corporate Law' },
        { value: 'CIVIL', label: 'Civil Law' },
        { value: 'LABOR', label: 'Labor Law' },
        { value: 'TAX', label: 'Tax Law' },
        { value: 'INTELLECTUAL_PROPERTY', label: 'Intellectual Property' },
        { value: 'OTHER', label: 'Other' }
    ];

    useEffect(() => {
        fetchCaseData();
    }, [caseId]);

    const fetchCaseData = async () => {
        try {
            const response = await casesApi.getById(caseId);
            setCaseData(response.data);
            setFormData({
                caseTitle: response.data.caseTitle || '',
                caseType: response.data.caseType || '',
                description: response.data.description || ''
            });
        } catch (error) {
            console.error('Error fetching case:', error);
            toast.error('Failed to load case details');
        } finally {
            setLoading(false);
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSave = async () => {
        try {
            await casesApi.update(caseId, formData);
            toast.success('Draft saved successfully');
            setEditing(false);
            fetchCaseData();
        } catch (error) {
            console.error('Error saving draft:', error);
            toast.error('Failed to save draft');
        }
    };

    const handlePublish = async () => {
        // Validate required fields
        if (!formData.caseTitle.trim()) {
            toast.error('Case title is required');
            return;
        }
        if (!formData.description.trim()) {
            toast.error('Case description is required');
            return;
        }

        setPublishing(true);
        try {
            // Save any pending changes first
            if (editing) {
                await casesApi.update(caseId, formData);
            }

            // Publish the case
            await casesApi.publish(caseId);
            toast.success('Case published successfully! Lawyers can now see your case.');
            if (onPublish) onPublish();
        } catch (error) {
            console.error('Error publishing case:', error);
            toast.error(error.response?.data?.message || 'Failed to publish case');
        } finally {
            setPublishing(false);
        }
    };

    const handlePlayAudio = async () => {
        const lang = audioLanguage;
        if (audioUrl[lang]) {
            const audio = new Audio(audioUrl[lang]);
            audio.play();
            return;
        }

        setAudioLoading(true);
        setAudioError(null);
        try {
            const response = await ttsApi.generate(caseId, lang);
            const base64Audio = response.data.audio;
            const audioBlob = await (await fetch(`data:audio/mp3;base64,${base64Audio}`)).blob();
            const url = URL.createObjectURL(audioBlob);
            setAudioUrl(prev => ({ ...prev, [lang]: url }));
            const audio = new Audio(url);
            audio.play();
        } catch (error) {
            console.error('Error generating audio:', error);
            setAudioError(`Failed to load ${lang === 'en' ? 'English' : 'Gujarati'} audio. Please try again.`);
        } finally {
            setAudioLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center p-20">
                <div className="text-center">
                    <div className="size-16 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
                    <p className="text-slate-500 font-bold uppercase tracking-widest text-xs">Loading draft...</p>
                </div>
            </div>
        );
    }

    if (!caseData) {
        return (
            <div className="p-8 text-center">
                <p className="text-red-500">Case not found</p>
            </div>
        );
    }

    return (
        <div className="max-w-5xl mx-auto space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-700">
            {/* Header */}
            <div className="bg-gradient-to-r from-primary to-blue-600 rounded-3xl p-8 text-white relative overflow-hidden">
                <div className="absolute top-0 right-0 size-64 bg-white/5 rounded-full -translate-y-1/2 translate-x-1/2"></div>
                <div className="relative z-10">
                    <div className="flex items-center gap-3 mb-2">
                        <span className="material-symbols-outlined text-3xl">draft</span>
                        <span className="px-3 py-1 bg-white/20 rounded-full text-xs font-black uppercase tracking-wider">
                            Draft Preview
                        </span>
                    </div>
                    <h2 className="text-3xl font-black mb-2">Review Your Case</h2>
                    <p className="text-blue-100 text-sm">
                        AI has analyzed your audio and generated this case draft. Review and edit the details before publishing to lawyers.
                    </p>
                    <div className="mt-4 flex flex-col gap-3">
                        <div className="flex items-center gap-2 bg-white/10 w-fit p-1 rounded-xl backdrop-blur-md border border-white/10">
                            <button
                                onClick={() => setAudioLanguage('en')}
                                className={`px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-widest transition-all ${audioLanguage === 'en' ? 'bg-white text-primary shadow-lg' : 'text-white/70 hover:text-white'}`}
                            >
                                English
                            </button>
                            <button
                                onClick={() => setAudioLanguage('gu')}
                                className={`px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-widest transition-all ${audioLanguage === 'gu' ? 'bg-white text-primary shadow-lg' : 'text-white/70 hover:text-white'}`}
                            >
                                ગુજરાતી
                            </button>
                        </div>
                        <button
                            onClick={handlePlayAudio}
                            disabled={audioLoading}
                            className="px-4 py-2 bg-white/20 hover:bg-white/30 rounded-xl text-white font-bold text-xs uppercase tracking-widest transition-all flex items-center gap-2 backdrop-blur-sm w-fit"
                        >
                            {audioLoading ? (
                                <>
                                    <div className="size-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                    Generating Audio...
                                </>
                            ) : (
                                <>
                                    <span className="material-symbols-outlined text-lg">volume_up</span>
                                    {audioUrl[audioLanguage] ? 'Play Audio Again' : 'Play Case Audio'}
                                </>
                            )}
                        </button>
                        {audioError && <p className="text-red-200 text-xs mt-2">{audioError}</p>}
                    </div>
                </div>
            </div>

            {/* Main Content */}
            <div className="bg-white dark:bg-gray-900 rounded-3xl border border-slate-200 dark:border-slate-800 overflow-hidden">
                <div className="p-8 space-y-6">
                    {/* Case Title */}
                    <div>
                        <label className="block text-xs font-black uppercase tracking-widest text-slate-500 mb-3">
                            Case Title
                        </label>
                        {editing ? (
                            <input
                                type="text"
                                name="caseTitle"
                                value={formData.caseTitle}
                                onChange={handleInputChange}
                                className="w-full px-4 py-3 rounded-xl border-2 border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-bold focus:border-primary focus:outline-none transition-colors"
                                placeholder="Enter case title"
                            />
                        ) : (
                            <h3 className="text-2xl font-bold text-slate-900 dark:text-white">
                                {caseData.caseTitle}
                            </h3>
                        )}
                    </div>

                    {/* Case Type */}
                    <div>
                        <label className="block text-xs font-black uppercase tracking-widest text-slate-500 mb-3">
                            Legal Category
                        </label>
                        {editing ? (
                            <select
                                name="caseType"
                                value={formData.caseType}
                                onChange={handleInputChange}
                                className="w-full px-4 py-3 rounded-xl border-2 border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-bold focus:border-primary focus:outline-none transition-colors"
                            >
                                <option value="">Select category</option>
                                {caseTypes.map(type => (
                                    <option key={type.value} value={type.value}>
                                        {type.label}
                                    </option>
                                ))}
                            </select>
                        ) : (
                            <div className="inline-flex items-center gap-2 px-4 py-2 bg-primary/10 text-primary rounded-xl border border-primary/20">
                                <span className="material-symbols-outlined text-lg">category</span>
                                <span className="font-bold">
                                    {caseTypes.find(t => t.value === caseData.caseType)?.label || caseData.caseType || 'Not specified'}
                                </span>
                            </div>
                        )}
                    </div>

                    {/* Description */}
                    <div>
                        <label className="block text-xs font-black uppercase tracking-widest text-slate-500 mb-3">
                            Case Description
                        </label>
                        {editing ? (
                            <textarea
                                name="description"
                                value={formData.description}
                                onChange={handleInputChange}
                                rows={8}
                                className="w-full px-4 py-3 rounded-xl border-2 border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-medium focus:border-primary focus:outline-none transition-colors resize-none"
                                placeholder="Describe your legal situation"
                            />
                        ) : (
                            <div className="p-6 bg-slate-50 dark:bg-slate-800 rounded-2xl">
                                <p className="text-slate-700 dark:text-slate-300 leading-relaxed whitespace-pre-wrap">
                                    {caseData.description}
                                </p>
                            </div>
                        )}
                    </div>


                </div>

                {/* Action Buttons */}
                <div className="px-8 py-6 bg-slate-50 dark:bg-slate-800/50 border-t border-slate-200 dark:border-slate-700 flex items-center justify-between gap-4">
                    <button
                        onClick={onCancel}
                        className="px-6 py-3 rounded-xl text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700 font-bold text-sm uppercase tracking-widest transition-all"
                    >
                        Cancel
                    </button>

                    <div className="flex items-center gap-3">
                        {editing ? (
                            <>
                                <button
                                    onClick={() => {
                                        setFormData({
                                            caseTitle: caseData.caseTitle || '',
                                            caseType: caseData.caseType || '',
                                            description: caseData.description || ''
                                        });
                                        setEditing(false);
                                    }}
                                    className="px-6 py-3 rounded-xl bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-300 dark:hover:bg-slate-600 font-bold text-sm uppercase tracking-widest transition-all"
                                >
                                    Discard Changes
                                </button>
                                <button
                                    onClick={handleSave}
                                    className="px-6 py-3 rounded-xl bg-primary text-white hover:opacity-90 font-bold text-sm uppercase tracking-widest transition-all shadow-lg shadow-primary/20"
                                >
                                    Save Draft
                                </button>
                            </>
                        ) : (
                            <>
                                <button
                                    onClick={() => setEditing(true)}
                                    className="px-6 py-3 rounded-xl bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-300 dark:hover:bg-slate-600 font-bold text-sm uppercase tracking-widest transition-all flex items-center gap-2"
                                >
                                    <span className="material-symbols-outlined text-lg">edit</span>
                                    Edit Draft
                                </button>
                                <button
                                    onClick={handlePublish}
                                    disabled={publishing}
                                    className="px-8 py-3 rounded-xl bg-gradient-to-r from-primary to-blue-600 text-white hover:opacity-90 font-bold text-sm uppercase tracking-widest transition-all shadow-lg shadow-primary/20 flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {publishing ? (
                                        <>
                                            <div className="size-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                            Publishing...
                                        </>
                                    ) : (
                                        <>
                                            <span className="material-symbols-outlined text-lg">publish</span>
                                            Publish Case
                                        </>
                                    )}
                                </button>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CaseDraftPreview;
