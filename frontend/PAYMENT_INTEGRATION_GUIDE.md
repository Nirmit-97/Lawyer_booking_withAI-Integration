# Frontend Payment Integration - Summary

## ✅ Components Created

### 1. API Services
- **`offersApi`** - Added to `api.js`
  - `submit(caseId, offerData)` - Lawyer submits offer
  - `getMyOffers()` - Lawyer views their offers
  - `getForCase(caseId)` - User views offers for a case
  - `accept(caseId, offerId)` - User accepts an offer

- **`paymentsApi`** - Added to `api.js`
  - `create(paymentData, idempotencyKey)` - Create payment order
  - `getStatus(paymentId)` - Get payment status

### 2. React Components

#### `OffersList.js` (User View)
- Displays all lawyer proposals for a case
- Shows lawyer details, fee, timeline, proposal message
- Status badges (Pending, Accepted, Rejected, etc.)
- "Accept Offer" button for submitted offers
- Responsive grid layout

#### `RazorpayCheckout.js` (Payment Processing)
- Loads Razorpay SDK dynamically
- Displays fee breakdown (lawyer fee, platform fee, gateway charges)
- Initiates Razorpay payment modal
- Handles success/failure callbacks
- Idempotency support

#### `SubmitOfferForm.js` (Lawyer View)
- Form for lawyers to submit proposals
- Fields: proposed fee, timeline, consultation type, message
- Validation and error handling
- Shows platform fee notice

### 3. Styling
- `OffersList.css` - Modern card-based design
- `RazorpayCheckout.css` - Payment UI styling
- `SubmitOfferForm.css` - Form styling

## 📋 Integration Instructions

To integrate into `CaseDetail.js`, add:

1. **Import components** at the top:
```javascript
import OffersList from './OffersList';
import RazorpayCheckout from './RazorpayCheckout';
import SubmitOfferForm from './SubmitOfferForm';
import { offersApi } from '../utils/api';
```

2. **Add state for offers and payment**:
```javascript
const [showOfferForm, setShowOfferForm] = useState(false);
const [showPayment, setShowPayment] = useState(false);
const [selectedOffer, setSelectedOffer] = useState(null);
```

3. **Add "Offers" tab** to the tab list:
```javascript
{['messages', 'offers', 'documents', 'timeline'].map(tab => (...))}
```

4. **Add offers tab content** in the tab content area:
```javascript
{activeTab === 'offers' && (
    <div>
        {/* For Users: Show offers list */}
        {!isLawyer && (
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
                
                {/* Show payment if offer accepted */}
                {showPayment && caseData.caseStatus === 'PAYMENT_PENDING' && (
                    <RazorpayCheckout
                        offerId={selectedOffer || caseData.selectedOfferId}
                        caseId={caseId}
                        onSuccess={(response, payment) => {
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
        )}
        
        {/* For Lawyers: Show offer submission form */}
        {isLawyer && (
            <>
                {(caseData.caseStatus === 'PUBLISHED' || caseData.caseStatus === 'UNDER_REVIEW') && !showOfferForm && (
                    <button 
                        onClick={() => setShowOfferForm(true)}
                        className="submit-offer-trigger-btn"
                    >
                        Submit Proposal
                    </button>
                )}
                
                {showOfferForm && (
                    <SubmitOfferForm
                        caseId={caseId}
                        onOfferSubmitted={() => {
                            setShowOfferForm(false);
                            toast.success('Offer submitted!');
                        }}
                        onCancel={() => setShowOfferForm(false)}
                    />
                )}
            </>
        )}
    </div>
)}
```

## 🎯 User Flow

### For Users (Clients):
1. Publish case → Status: `PUBLISHED`
2. Lawyers submit offers → Status: `UNDER_REVIEW`
3. User views offers in "Offers" tab
4. User accepts one offer → Status: `PAYMENT_PENDING`
5. Payment component appears automatically
6. User completes Razorpay payment
7. On success → Status: `IN_PROGRESS`
8. Case proceeds normally

### For Lawyers:
1. View published cases
2. Click "Submit Proposal" in offers tab
3. Fill form (fee, timeline, message)
4. Submit offer
5. Wait for user acceptance
6. If accepted, case moves to `IN_PROGRESS` after payment

## 🔐 Razorpay Integration

The `RazorpayCheckout` component:
- Loads Razorpay SDK from CDN
- Uses test key: `rzp_test_SETpSTi3g34IKY`
- Creates payment order via backend
- Opens Razorpay modal for payment
- Handles webhook verification on backend

## 📱 Responsive Design

All components are mobile-responsive:
- Grid layouts adapt to screen size
- Forms stack vertically on mobile
- Touch-friendly buttons and inputs

## ⚠️ Important Notes

1. **Razorpay Test Mode**: Currently using test credentials
2. **Webhook Setup**: Requires ngrok for local testing
3. **Production**: Move API keys to environment variables
4. **Database**: Run migrations before testing

## 🎨 Design Features

- Modern glassmorphism effects
- Gradient buttons
- Status badges with colors
- Smooth animations and transitions
- Dark mode compatible (if implemented)
