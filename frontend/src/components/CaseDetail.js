import React, { useState, useEffect, useCallback } from 'react';
import UserCaseMessages from './UserCaseMessages';
import LawyerProfile from './LawyerProfile';
import { casesApi } from '../utils/api';
import { toast } from 'react-toastify';
import PaymentButton from './PaymentButton';

const CaseDetail = ({ caseId, userType, userId, lawyerId, onBack }) => {
    const [caseData, setCaseData] = useState(null);
    const [solution, setSolution] = useState('');
    const [loading, setLoading] = useState(true);
    const [savingSolution, setSavingSolution] = useState(false);
    const [error, setError] = useState('');

    const fetchCaseDetails = useCallback(async () => {
        setLoading(true);
        try {
            // If we don't have the full object, fetch it
            const response = await casesApi.getById(caseId);
            if (response.data) {
                setCaseData(response.data);
                setSolution(response.data.solution || '');
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

    const handleSaveSolution = async () => {
        if (!solution.trim()) {
            toast.warning('Solution cannot be empty');
            return;
        }

        setSavingSolution(true);
        try {
            await casesApi.updateSolution(caseId, solution);
            const updatedCase = { ...caseData, solution, caseStatus: 'solved' };
            setCaseData(updatedCase);

            // Also update status to solved/closed if not already
            if (caseData.caseStatus !== 'solved') {
                await casesApi.updateStatus(caseId, 'solved');
            }

            toast.success('Solution saved successfully!');
        } catch (err) {
            console.error('Error saving solution:', err);
            toast.error('Failed to save solution.');
        } finally {
            setSavingSolution(false);
        }
    };

    if (loading) return <div className="p-4">Loading case details...</div>;
    if (error) return <div className="p-4 text-red-500">{error} <button onClick={onBack}>Go Back</button></div>;
    if (!caseData) return null;

    const isLawyer = userType === 'lawyer';

    return (
        <div className="case-detail-container" style={{ padding: '20px', background: '#fff', borderRadius: '8px' }}>
            <button
                onClick={onBack}
                style={{
                    marginBottom: '20px',
                    background: 'transparent',
                    border: 'none',
                    color: '#3498db',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    fontSize: '1rem'
                }}
            >
                &larr; Back to List
            </button>

            <div style={{ borderBottom: '1px solid #eee', paddingBottom: '20px', marginBottom: '20px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <h2 style={{ margin: 0 }}>{caseData.caseTitle}</h2>
                    <span style={{
                        padding: '6px 12px',
                        borderRadius: '20px',
                        background: caseData.caseStatus === 'open' ? '#e3f2fd' :
                            caseData.caseStatus === 'payment_pending' ? '#fff3e0' :
                                caseData.caseStatus === 'payment_received' ? '#e8f5e9' : '#e8f5e9',
                        color: caseData.caseStatus === 'open' ? '#1976d2' :
                            caseData.caseStatus === 'payment_pending' ? '#ef6c00' :
                                caseData.caseStatus === 'payment_received' ? '#2e7d32' : '#2e7d32',
                        fontWeight: 'bold'
                    }}>
                        {caseData.caseStatus.toUpperCase().replace('_', ' ')}
                    </span>
                </div>
                {/* Payment Section */}
                {!isLawyer && caseData.caseStatus.toUpperCase() === 'PAYMENT_PENDING' && (
                    <div style={{ marginTop: '20px', padding: '15px', background: '#fff8e1', border: '1px solid #ffecb3', borderRadius: '8px' }}>
                        <p style={{ margin: '0 0 10px 0', color: '#f57f17' }}><strong>Action Required:</strong> Please pay the consultation fee to proceed.</p>

                        <div style={{ margin: '10px 0', padding: '10px', background: 'white', borderRadius: '5px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span>Lawyer Consultation Fee:</span>
                                <span>₹{caseData.lawyerFee || 500}</span>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', color: '#666', fontSize: '0.9em' }}>
                                <span>Platform Fee (10%):</span>
                                <span>₹{((caseData.lawyerFee || 500) * 0.10).toFixed(2)}</span>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold', borderTop: '1px solid #eee', marginTop: '5px', paddingTop: '5px' }}>
                                <span>Total Amount:</span>
                                <span>₹{((caseData.lawyerFee || 500) * 1.10).toFixed(2)}</span>
                            </div>
                        </div>

                        <PaymentButton caseId={caseId} onPaymentSuccess={fetchCaseDetails} />
                    </div>
                )}
                {caseData.caseStatus.toUpperCase() === 'PAYMENT_RECEIVED' && (
                    <div style={{ marginTop: '20px', padding: '15px', background: '#f1f8e9', border: '1px solid #dcedc8', borderRadius: '8px' }}>
                        <p style={{ margin: 0, color: '#33691e' }}><strong>Payment Received!</strong> You can now consult with the lawyer.</p>
                    </div>
                )}

                <p style={{ color: '#666', marginTop: '10px' }}>
                    <strong>Case ID:</strong> {caseData.id} |
                    <strong> Created:</strong> {new Date(caseData.createdAt).toLocaleString()}
                </p>
            </div>

            <div className="case-description" style={{ marginBottom: '30px', padding: '15px', background: '#f8f9fa', borderRadius: '8px' }}>
                <h3 style={{ marginTop: 0, fontSize: '1.1rem' }}>Description</h3>
                <p style={{ lineHeight: '1.6', color: '#2c3e50' }}>{caseData.description}</p>
            </div>

            <div className="case-solution" style={{ marginBottom: '30px' }}>
                <h3 style={{ fontSize: '1.1rem', borderLeft: '4px solid #27ae60', paddingLeft: '10px' }}>Legal Solution</h3>

                {isLawyer ? (
                    <div className="editor-section">
                        <textarea
                            value={solution}
                            onChange={(e) => setSolution(e.target.value)}
                            placeholder="Write your professional legal advice here..."
                            style={{
                                width: '100%',
                                minHeight: '150px',
                                padding: '15px',
                                borderRadius: '8px',
                                border: '1px solid #ccc',
                                marginBottom: '10px',
                                fontFamily: 'inherit',
                                resize: 'vertical'
                            }}
                        />
                        <button
                            onClick={handleSaveSolution}
                            disabled={savingSolution}
                            style={{
                                padding: '10px 20px',
                                backgroundColor: '#27ae60',
                                color: 'white',
                                border: 'none',
                                borderRadius: '5px',
                                cursor: savingSolution ? 'not-allowed' : 'pointer',
                                fontWeight: 'bold',
                                opacity: savingSolution ? 0.7 : 1
                            }}
                        >
                            {savingSolution ? 'Saving...' : 'Submit Solution'}
                        </button>
                    </div>
                ) : (
                    <div style={{
                        padding: '20px',
                        background: solution ? '#e8f5e9' : '#fff3e0',
                        borderRadius: '8px',
                        border: solution ? '1px solid #c8e6c9' : '1px solid #ffe0b2'
                    }}>
                        {solution ? (
                            <p style={{ whiteSpace: 'pre-wrap', margin: 0 }}>{solution}</p>
                        ) : (
                            <p style={{ color: '#f57c00', margin: 0 }}>
                                <i>Pending lawyer review. A solution will appear here once the assigned lawyer processes your case.</i>
                            </p>
                        )}
                    </div>
                )}
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px' }}>
                <div className="case-messages">
                    <UserCaseMessages
                        caseId={caseId}
                        userId={userId}
                        userType={userType}
                        lawyerId={caseData.lawyerId}
                        clientUserId={caseData.userId}
                        onCaseUpdate={(updatedCase) => {
                            setCaseData(updatedCase);
                            if (updatedCase.solution !== undefined) {
                                setSolution(updatedCase.solution);
                            }
                        }}
                    />
                </div>

                {!isLawyer && caseData.lawyerId && (
                    <div className="lawyer-expert-profile">
                        <h3 style={{ fontSize: '1.1rem', marginBottom: '15px' }}>Your Assigned Lawyer</h3>
                        <LawyerProfile lawyerId={caseData.lawyerId} />
                    </div>
                )}
            </div>
        </div>
    );
};

export default CaseDetail;
