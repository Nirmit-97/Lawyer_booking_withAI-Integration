import React, { useState, useEffect } from 'react';
import { paymentsApi } from '../utils/api';
import './RazorpayCheckout.css';

const RAZORPAY_KEY_ID = 'rzp_test_SETpSTi3g34IKY'; // This will come from backend

const RazorpayCheckout = ({ offerId, caseId, onSuccess, onFailure }) => {
    const [loading, setLoading] = useState(false);
    const [paymentData, setPaymentData] = useState(null);
    const [error, setError] = useState(null);

    // Load Razorpay script
    useEffect(() => {
        const script = document.createElement('script');
        script.src = 'https://checkout.razorpay.com/v1/checkout.js';
        script.async = true;
        document.body.appendChild(script);

        return () => {
            document.body.removeChild(script);
        };
    }, []);

    const fetchPaymentData = async () => {
        try {
            setLoading(true);
            const idempotencyKey = `${caseId}-${offerId}-preview`;
            const response = await paymentsApi.create({ offerId }, idempotencyKey);
            setPaymentData(response.data);
        } catch (err) {
            console.error('Error fetching payment preview:', err);
            setError('Failed to load payment details.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchPaymentData();
    }, [offerId]);

    const handlePayment = async () => {
        try {
            if (!paymentData) {
                await fetchPaymentData();
            }
            // Use existing paymentData or fetch new if needed
            const payment = paymentData;

            const options = {
                key: payment.razorpayKeyId || RAZORPAY_KEY_ID,
                amount: payment.totalAmount * 100, // Convert to paise
                currency: 'INR',
                name: 'Legal Connect',
                description: `Payment for Case #${caseId}`,
                order_id: payment.gatewayOrderId,
                handler: async function (response) {
                    // Payment successful locally, now verify with backend
                    console.log('Payment successful, verifying with backend:', response);
                    setLoading(true);
                    try {
                        await paymentsApi.verify({
                            razorpayPaymentId: response.razorpay_payment_id,
                            razorpayOrderId: response.razorpay_order_id,
                            razorpaySignature: response.razorpay_signature
                        });

                        console.log('Payment verified successfully');
                        if (onSuccess) {
                            onSuccess(response, payment);
                        }
                    } catch (verifyErr) {
                        console.error('Verification failed:', verifyErr);
                        setError('Payment verification failed. Please contact support.');
                        if (onFailure) {
                            onFailure('Verification failed');
                        }
                    } finally {
                        setLoading(false);
                    }
                },
                prefill: {
                    name: '',
                    email: '',
                    contact: ''
                },
                theme: {
                    color: '#2196f3'
                },
                modal: {
                    ondismiss: function () {
                        console.log('Payment cancelled by user');
                        if (onFailure) {
                            onFailure('Payment cancelled');
                        }
                    }
                }
            };

            const razorpay = new window.Razorpay(options);
            razorpay.on('payment.failed', function (response) {
                console.error('Payment failed:', response.error);
                if (onFailure) {
                    onFailure(response.error.description || 'Payment failed');
                }
            });

            razorpay.open();
        } catch (err) {
            console.error('Error initiating payment:', err);
        }
    };

    return (
        <div className="razorpay-checkout">
            {error && (
                <div className="payment-error">
                    <p>{error}</p>
                </div>
            )}

            {paymentData && (
                <div className="payment-breakdown">
                    <h4>Payment Breakdown</h4>
                    <div className="breakdown-row">
                        <span>Lawyer Fee:</span>
                        <span>₹{paymentData.lawyerFee?.toLocaleString()}</span>
                    </div>
                    <div className="breakdown-row">
                        <span>Gateway Charges:</span>
                        <span>₹{paymentData.gatewayFee?.toLocaleString()}</span>
                    </div>
                    <div className="breakdown-row total">
                        <span>Total Amount:</span>
                        <span>₹{paymentData.totalAmount?.toLocaleString()}</span>
                    </div>
                    <p className="platform-fee-note">
                        Platform fee (₹{paymentData.platformFee?.toLocaleString()}) will be deducted from lawyer's payout
                    </p>
                </div>
            )}

            <button
                className="proceed-payment-btn"
                onClick={handlePayment}
                disabled={loading}
            >
                {loading ? 'Processing...' : 'Proceed to Payment'}
            </button>

            <div className="payment-info">
                <p>🔒 Secure payment powered by Razorpay</p>
                <p>Your payment will be held in escrow until case completion</p>
            </div>
        </div>
    );
};

export default RazorpayCheckout;
