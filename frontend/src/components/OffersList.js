import React, { useState, useEffect } from 'react';
import { offersApi } from '../utils/api';
import { toast } from 'react-toastify';
import { PremiumModal } from './FeedbackModal';
import './OffersList.css';

const OffersList = ({ caseId, caseStatus, onOfferAccepted }) => {
    const [offers, setOffers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [accepting, setAccepting] = useState(null);
    const [showAcceptModal, setShowAcceptModal] = useState(false);
    const [selectedOfferId, setSelectedOfferId] = useState(null);

    useEffect(() => {
        fetchOffers();
    }, [caseId]);

    const fetchOffers = async () => {
        try {
            setLoading(true);
            const response = await offersApi.getForCase(caseId);
            setOffers(response.data || []);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to load offers');
        } finally {
            setLoading(false);
        }
    };

    const handleAcceptOffer = (offerId) => {
        setSelectedOfferId(offerId);
        setShowAcceptModal(true);
    };

    const confirmAcceptOffer = async () => {
        const offerId = selectedOfferId;
        try {
            setAccepting(offerId);
            await offersApi.accept(caseId, offerId);
            toast.success('Offer accepted! Please proceed to payment.');
            if (onOfferAccepted) {
                onOfferAccepted(offerId);
            }
            fetchOffers(); // Refresh offers
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to accept offer');
        } finally {
            setAccepting(null);
            setShowAcceptModal(false);
            setSelectedOfferId(null);
        }
    };

    const getStatusBadge = (status) => {
        const badges = {
            SUBMITTED: { class: 'badge-submitted', text: 'Pending' },
            ACCEPTED: { class: 'badge-accepted', text: 'Accepted' },
            REJECTED: { class: 'badge-rejected', text: 'Rejected' },
            EXPIRED: { class: 'badge-expired', text: 'Expired' },
            FUNDED: { class: 'badge-funded', text: 'Funded' },
        };
        const badge = badges[status] || { class: '', text: status };
        return <span className={`offer-status-badge ${badge.class}`}>{badge.text}</span>;
    };

    if (loading) {
        return <div className="offers-loading">Loading offers...</div>;
    }

    if (error) {
        return <div className="offers-error">{error}</div>;
    }

    if (offers.length === 0) {
        return (
            <div className="no-offers">
                <p>No offers received yet. Lawyers will submit their proposals soon.</p>
            </div>
        );
    }

    const canAcceptOffers = caseStatus === 'UNDER_REVIEW' || caseStatus === 'PENDING_APPROVAL';

    return (
        <div className="offers-list">
            <h3>Lawyer Proposals ({offers.length})</h3>
            <div className="offers-grid">
                {offers.map((offer) => (
                    <div key={offer.id} className={`offer-card ${offer.status === 'ACCEPTED' ? 'accepted' : ''}`}>
                        <div className="offer-header">
                            <div className="lawyer-info">
                                <h4>{offer.lawyerName || 'Lawyer'}</h4>
                                {offer.lawyerSpecialization && (
                                    <p className="specialization">{offer.lawyerSpecialization}</p>
                                )}
                                <div className="lawyer-meta">
                                    {offer.lawyerExperience && (
                                        <span className="experience">{offer.lawyerExperience} years exp.</span>
                                    )}
                                    {offer.lawyerRating ? (
                                        <span className="rating">⭐ {offer.lawyerRating.toFixed(1)}</span>
                                    ) : (
                                        <span className="rating">⭐ New</span>
                                    )}
                                    <button
                                        className="view-profile-link"
                                        onClick={() => window.open(`/lawyer/${offer.lawyerId}?caseId=${caseId}`, '_blank')}
                                    >
                                        View Profile
                                    </button>
                                </div>
                            </div>
                            {getStatusBadge(offer.status)}
                        </div>

                        <div className="offer-details">
                            <div className="fee-section">
                                <span className="fee-label">Proposed Fee:</span>
                                <span className="fee-amount">₹{offer.proposedFee.toLocaleString()}</span>
                            </div>

                            {offer.estimatedTimeline && (
                                <div className="timeline-section">
                                    <span className="timeline-label">Timeline:</span>
                                    <span className="timeline-value">{offer.estimatedTimeline}</span>
                                </div>
                            )}

                            {offer.consultationType && (
                                <div className="consultation-type">
                                    <span className="type-label">Consultation:</span>
                                    <span className="type-value">{offer.consultationType}</span>
                                </div>
                            )}

                            {offer.proposalMessage && (
                                <div className="proposal-message">
                                    <p className="message-label">Proposal:</p>
                                    <p className="message-text">{offer.proposalMessage}</p>
                                </div>
                            )}

                            <div className="offer-footer">
                                <span className="offer-date">
                                    Submitted: {new Date(offer.createdAt).toLocaleDateString()}
                                </span>
                                {offer.status === 'SUBMITTED' && offer.expiresAt && (
                                    <span className="expires-at">
                                        Expires: {new Date(offer.expiresAt).toLocaleDateString()}
                                    </span>
                                )}
                            </div>
                        </div>

                        {canAcceptOffers && offer.status === 'SUBMITTED' && (
                            <button
                                className="accept-offer-btn"
                                onClick={() => handleAcceptOffer(offer.id)}
                                disabled={accepting === offer.id}
                            >
                                {accepting === offer.id ? 'Accepting...' : 'Accept Offer'}
                            </button>
                        )}
                    </div>
                ))}
            </div>

            <PremiumModal
                isOpen={showAcceptModal}
                onClose={() => setShowAcceptModal(false)}
                onConfirm={confirmAcceptOffer}
                title="Accept Offer"
                message="Are you sure you want to accept this offer? This will officially appoint the lawyer and reject all other pending proposals."
                confirmText="Accept & Proceed"
                variant="success"
            />
        </div>
    );
};

export default OffersList;
