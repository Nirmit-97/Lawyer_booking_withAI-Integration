import React, { useState } from 'react';
import { paymentApi } from '../utils/api';
import { toast } from 'react-toastify';

const PaymentButton = ({ caseId, onPaymentSuccess }) => {
    const [loading, setLoading] = useState(false);

    const handlePayment = async () => {
        setLoading(true);
        try {
            // 1. Create Order
            const orderResponse = await paymentApi.createOrder(caseId);
            const { orderId, amount, currency, keyId } = orderResponse.data;

            const options = {
                key: keyId, // Enter the Key ID generated from the Dashboard
                amount: amount,
                currency: currency,
                name: "Legal Connect",
                description: "Legal Consultation Fee",
                // image: "https://example.com/your_logo",
                order_id: orderId,
                handler: async function (response) {
                    // 2. Verify Payment
                    try {
                        const verifyResponse = await paymentApi.verifyPayment({
                            razorpay_order_id: response.razorpay_order_id,
                            razorpay_payment_id: response.razorpay_payment_id,
                            razorpay_signature: response.razorpay_signature
                        });

                        if (verifyResponse.data.status === 'PAID') {
                            toast.success('Payment Successful!');
                            if (onPaymentSuccess) onPaymentSuccess();
                        } else {
                            toast.error('Payment verification failed.');
                        }
                    } catch (err) {
                        console.error('Verification error:', err);
                        toast.error('Payment verification failed on server.');
                    }
                },
                prefill: {
                    name: "User Name", // You can pass user details here if available
                    email: "user@example.com",
                    contact: "9999999999"
                },
                notes: {
                    address: "Legal Connect Corporate Office"
                },
                theme: {
                    color: "#3399cc"
                },
                config: {
                    display: {
                        blocks: {
                            upi: {
                                name: "Pay via UPI",
                                instruments: [
                                    {
                                        method: "upi"
                                    }
                                ]
                            },
                            qr: {
                                name: "Pay via QR",
                                instruments: [
                                    {
                                        method: "upi",
                                        flow: "qr"
                                    }
                                ]
                            },
                            other: {
                                name: "Other Methods",
                                instruments: [
                                    { method: "card" },
                                    { method: "netbanking" }
                                ]
                            }
                        },
                        sequence: ["block.upi", "block.qr", "block.other"],
                        preferences: {
                            show_default_blocks: true
                        }
                    }
                }
            };

            const rzp1 = new window.Razorpay(options);
            rzp1.on('payment.failed', function (response) {
                toast.error(`Payment Failed: ${response.error.description}`);
            });
            rzp1.open();

        } catch (err) {
            console.error('Order creation error:', err);
            toast.error('Failed to initiate payment. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <button
            onClick={handlePayment}
            disabled={loading}
            style={{
                padding: '10px 20px',
                backgroundColor: '#673ab7',
                color: 'white',
                border: 'none',
                borderRadius: '5px',
                fontWeight: 'bold',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.7 : 1,
                marginTop: '10px'
            }}
        >
            {loading ? 'Processing...' : 'Pay Consultation Fee'}
        </button>
    );
};

export default PaymentButton;
