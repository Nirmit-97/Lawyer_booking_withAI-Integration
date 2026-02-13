import React, { useState } from 'react';
import { reviewsApi } from '../utils/api';
import { toast } from 'react-toastify';

const FeedbackModal = ({ caseId, lawyerId, userId, onClose, onSubmitSuccess }) => {
    const [rating, setRating] = useState(0);
    const [hover, setHover] = useState(0);
    const [comment, setComment] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async () => {
        if (rating === 0) {
            toast.warning('Please select a rating');
            return;
        }

        setSubmitting(true);
        try {
            await reviewsApi.submit({
                caseId,
                lawyerId,
                userId,
                rating,
                comment
            });
            toast.success('Thank you for your feedback!');
            onSubmitSuccess();
            onClose();
        } catch (err) {
            console.error('Error submitting feedback:', err);
            toast.error('Failed to submit feedback');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-primary/20 backdrop-blur-xl animate-in fade-in duration-300">
            <div className="bg-white dark:bg-slate-900 w-full max-w-lg rounded-[2.5rem] p-10 shadow-2xl border border-white/50 dark:border-slate-800 relative overflow-hidden">
                {/* Decorative background */}
                <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full -mr-16 -mt-16"></div>

                <div className="relative z-10 space-y-8">
                    <div className="text-center space-y-4">
                        <div className="w-20 h-20 bg-emerald-500/10 rounded-3xl flex items-center justify-center mx-auto text-emerald-500 mb-6 group">
                            <span className="material-symbols-outlined text-4xl group-hover:scale-110 transition-transform">recommend</span>
                        </div>
                        <h2 className="text-3xl font-black text-slate-900 dark:text-white tracking-tight leading-tight uppercase">Rate Your Experience</h2>
                        <p className="text-sm font-medium text-slate-500 dark:text-slate-400">Your feedback helps us maintain the integrity of our legal protocol and recognizes expert counsel.</p>
                    </div>

                    <div className="flex justify-center gap-2">
                        {[1, 2, 3, 4, 5].map((star) => (
                            <button
                                key={star}
                                className={`text-4xl transition-all ${(hover || rating) >= star ? 'text-amber-400 scale-110' : 'text-slate-200 dark:text-slate-800'
                                    }`}
                                onClick={() => setRating(star)}
                                onMouseEnter={() => setHover(star)}
                                onMouseLeave={() => setHover(0)}
                            >
                                <span className={`material-symbols-outlined !text-4xl ${(hover || rating) >= star ? 'fill-icon' : ''}`}>
                                    star
                                </span>
                            </button>
                        ))}
                    </div>

                    <div className="space-y-4">
                        <label className="text-[10px] font-black uppercase tracking-widest text-slate-400 block ml-1">Professional Commentary</label>
                        <textarea
                            value={comment}
                            onChange={(e) => setComment(e.target.value)}
                            className="w-full h-32 bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 rounded-3xl p-5 text-sm font-medium focus:ring-2 focus:ring-primary outline-none text-slate-700 dark:text-slate-200 transition-all"
                            placeholder="Share your thoughts on the counsel's performance..."
                        />
                    </div>

                    <div className="flex gap-4">
                        <button
                            onClick={onClose}
                            className="flex-1 py-4 px-6 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 rounded-2xl text-[10px] font-black uppercase tracking-widest hover:bg-slate-200 transition-all"
                        >
                            Skip for now
                        </button>
                        <button
                            onClick={handleSubmit}
                            disabled={submitting}
                            className="flex-1 py-4 px-6 bg-primary text-white rounded-2xl text-[10px] font-black uppercase tracking-widest shadow-xl shadow-primary/20 hover:bg-blue-600 transition-all disabled:opacity-50"
                        >
                            {submitting ? 'Transmitting...' : 'Submit Review'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default FeedbackModal;
