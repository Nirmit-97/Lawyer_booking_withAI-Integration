import React, { useState, useEffect } from 'react';
import { offersApi } from '../utils/api';
import { toast } from 'react-toastify';
import { PremiumModal } from './FeedbackModal';
import './SubmitOfferForm.css';

const SubmitOfferForm = ({ caseId, onOfferSubmitted, onCancel }) => {
    const [formData, setFormData] = useState({
        proposedFee: '',
        estimatedTimeline: '',
        proposalMessage: '',
        consultationType: 'VIRTUAL',
    });
    const [existingOffer, setExistingOffer] = useState(null);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState(null);
    const [showWithdrawModal, setShowWithdrawModal] = useState(false);

    useEffect(() => {
        fetchExistingOffer();
    }, [caseId]);

    const fetchExistingOffer = async () => {
        try {
            setLoading(true);
            const response = await offersApi.getMyOffers();
            const myOfferForCase = response.data.find(o => o.caseId === parseInt(caseId));
            if (myOfferForCase && myOfferForCase.status !== 'WITHDRAWN') {
                setExistingOffer(myOfferForCase);
            } else {
                setExistingOffer(null);
            }
        } catch (err) {
            console.error('Error fetching existing offer:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.proposedFee || parseFloat(formData.proposedFee) <= 0) {
            setError('Please enter a valid fee amount');
            return;
        }

        try {
            setSubmitting(true);
            setError(null);

            const offerData = {
                proposedFee: parseFloat(formData.proposedFee),
                estimatedTimeline: formData.estimatedTimeline,
                proposalMessage: formData.proposalMessage,
                consultationType: formData.consultationType,
            };

            await offersApi.submit(caseId, offerData);
            toast.success('Offer submitted successfully!');

            if (onOfferSubmitted) {
                onOfferSubmitted();
            }
            fetchExistingOffer();
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to submit offer');
        } finally {
            setSubmitting(false);
        }
    };

    const handleWithdraw = async () => {
        setShowWithdrawModal(true);
    };

    const confirmWithdraw = async () => {
        try {
            setSubmitting(true);
            await offersApi.withdraw(existingOffer.id);
            toast.success('Proposal withdrawn successfully');
            setExistingOffer(null);
            if (onOfferSubmitted) onOfferSubmitted(); // Refresh parent view
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to withdraw proposal');
        } finally {
            setSubmitting(false);
            setShowWithdrawModal(false);
        }
    };

    if (loading) {
        return <div className="submit-offer-loading">Checking your existing proposals...</div>;
    }

    if (existingOffer) {
        return (
            <div className="existing-offer-view animate-in fade-in slide-in-from-bottom-2">
                <div className="offer-header">
                    <h3>Your Active Proposal</h3>
                    <span className={`status-badge ${existingOffer.status.toLowerCase()}`}>
                        {existingOffer.status}
                    </span>
                </div>

                <div className="offer-summary">
                    <div className="summary-item">
                        <label>Proposed Fee</label>
                        <p>₹{existingOffer.proposedFee.toLocaleString()}</p>
                    </div>
                    <div className="summary-item">
                        <label>Timeline</label>
                        <p>{existingOffer.estimatedTimeline || 'Not specified'}</p>
                    </div>
                    <div className="summary-item">
                        <label>Consultation</label>
                        <p>{existingOffer.consultationType}</p>
                    </div>
                </div>

                <div className="summary-item message">
                    <label>Proposal Message</label>
                    <p>{existingOffer.proposalMessage}</p>
                </div>

                {existingOffer.status === 'SUBMITTED' && (
                    <div className="offer-actions">
                        <button
                            className="withdraw-btn"
                            onClick={handleWithdraw}
                            disabled={submitting}
                        >
                            {submitting ? 'Withdrawing...' : 'Withdraw Proposal'}
                        </button>
                        <p className="withdraw-note">
                            * You can withdraw this proposal any time before the user accepts it.
                        </p>
                    </div>
                )}

                <PremiumModal
                    isOpen={showWithdrawModal}
                    onClose={() => setShowWithdrawModal(false)}
                    onConfirm={confirmWithdraw}
                    title="Withdraw Proposal"
                    message="Are you sure you want to withdraw this proposal? This action cannot be undone."
                    confirmText="Withdraw"
                    variant="danger"
                />
            </div>
        );
    }

    return (
        <div className="submit-offer-form animate-in fade-in slide-in-from-bottom-4">
            <h3>Submit Your Proposal</h3>

            {error && (
                <div className="form-error">
                    {error}
                </div>
            )}

            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="proposedFee">Proposed Fee (₹) *</label>
                    <input
                        type="number"
                        id="proposedFee"
                        name="proposedFee"
                        value={formData.proposedFee}
                        onChange={handleChange}
                        placeholder="Enter your fee"
                        required
                        min="0"
                        step="100"
                    />
                    <small>Platform fee (10%) and gateway charges (2%) will be applicable</small>
                </div>

                <div className="form-group">
                    <label htmlFor="estimatedTimeline">Estimated Timeline</label>
                    <input
                        type="text"
                        id="estimatedTimeline"
                        name="estimatedTimeline"
                        value={formData.estimatedTimeline}
                        onChange={handleChange}
                        placeholder="e.g., 2-3 weeks, 1 month"
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="consultationType">Consultation Type</label>
                    <select
                        id="consultationType"
                        name="consultationType"
                        value={formData.consultationType}
                        onChange={handleChange}
                    >
                        <option value="VIRTUAL">Virtual</option>
                        <option value="IN_PERSON">In Person</option>
                        <option value="HYBRID">Hybrid</option>
                    </select>
                </div>

                <div className="form-group">
                    <label htmlFor="proposalMessage">Proposal Message</label>
                    <textarea
                        id="proposalMessage"
                        name="proposalMessage"
                        value={formData.proposalMessage}
                        onChange={handleChange}
                        placeholder="Explain your approach, experience with similar cases, and why you're the best fit..."
                        rows="6"
                    />
                </div>

                <div className="form-actions">
                    <button
                        type="button"
                        className="cancel-btn"
                        onClick={onCancel}
                        disabled={submitting}
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        className="submit-btn"
                        disabled={submitting}
                    >
                        {submitting ? 'Submitting...' : 'Submit Offer'}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default SubmitOfferForm;
