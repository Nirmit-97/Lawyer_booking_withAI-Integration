# Payment System - Testing & Verification Guide

## 🚀 Quick Start

### Step 1: Start Backend
```bash
cd backend
mvn spring-boot:run
```

**Verify**: Backend should start on `http://localhost:8080`
- Check console for "Started LawyerBookingApplication"
- No compilation errors

### Step 2: Start Frontend
```bash
cd frontend
npm start
```

**Verify**: Frontend should open on `http://localhost:3000`

## ✅ Backend Verification

### 1. Check Database Tables
After backend starts, verify these tables exist:
```sql
SELECT * FROM offers;
SELECT * FROM payments;
SELECT * FROM cases; -- Should have new columns: selected_offer_id, offer_count
```

### 2. Test API Endpoints (Using Postman/cURL)

#### Test Offer Submission (Lawyer)
```bash
POST http://localhost:8080/api/lawyer/offers/cases/1
Authorization: Bearer {lawyer-jwt-token}
Content-Type: application/json

{
  "proposedFee": 50000,
  "estimatedTimeline": "2-3 weeks",
  "proposalMessage": "I have 10 years experience in this area",
  "consultationType": "VIRTUAL"
}
```

**Expected Response**: 200 OK with offer details

#### Test Get Offers (User)
```bash
GET http://localhost:8080/api/user/cases/1/offers
Authorization: Bearer {user-jwt-token}
```

**Expected Response**: Array of offers

#### Test Accept Offer (User)
```bash
POST http://localhost:8080/api/user/cases/1/offers/1/accept
Authorization: Bearer {user-jwt-token}
```

**Expected Response**: 200 OK, case status → PAYMENT_PENDING

#### Test Create Payment (User)
```bash
POST http://localhost:8080/api/payments/create
Authorization: Bearer {user-jwt-token}
Content-Type: application/json
Idempotency-Key: test-key-123

{
  "offerId": 1
}
```

**Expected Response**: Payment details with Razorpay order ID

### 3. Check Logs
Look for these in backend console:
```
✅ "Creating Razorpay order for amount: ..."
✅ "Payment created successfully with ID: ..."
✅ "Webhook received for payment: ..."
```

## 🎨 Frontend Verification

### 1. User Flow Testing

#### A. Create and Publish Case
1. Login as **User**
2. Create a new case
3. Publish the case
4. **Verify**: Case status shows "PUBLISHED"

#### B. View Case (Lawyer)
1. Login as **Lawyer**
2. Navigate to published cases
3. Open the case
4. Click **"Offers"** tab
5. **Verify**: "Submit Your Proposal" button appears

#### C. Submit Offer (Lawyer)
1. Click "Submit Your Proposal"
2. Fill the form:
   - Proposed Fee: 50000
   - Timeline: "2-3 weeks"
   - Consultation Type: Virtual
   - Message: "I can help with this case"
3. Click "Submit Offer"
4. **Verify**: 
   - Success message appears
   - Form closes
   - Case status → "UNDER_REVIEW"

#### D. View Offers (User)
1. Login as **User**
2. Open the case
3. Click **"Offers"** tab
4. **Verify**:
   - Offer card displays with lawyer details
   - Fee shows ₹50,000
   - Timeline shows "2-3 weeks"
   - "Accept Offer" button visible

#### E. Accept Offer (User)
1. Click "Accept Offer" on an offer
2. Confirm in popup
3. **Verify**:
   - Success message
   - Offer status → "ACCEPTED"
   - Other offers → "REJECTED"
   - Case status → "PAYMENT_PENDING"
   - Payment component appears below

#### F. Process Payment (User)
1. Review payment breakdown:
   - Lawyer Fee: ₹50,000
   - Gateway Fee: ₹1,000 (2%)
   - Total: ₹51,000
2. Click "Proceed to Payment"
3. **Verify**: Razorpay modal opens

#### G. Complete Payment
Use Razorpay test card:
- **Card**: 4111 1111 1111 1111
- **CVV**: 123
- **Expiry**: 12/25

Click "Pay Now"

**Verify**:
- Payment success message
- Case status → "IN_PROGRESS"
- Offer status → "FUNDED"

### 2. Component Verification

#### OffersList Component
- [ ] Displays all offers in grid layout
- [ ] Shows lawyer name, specialization, experience, rating
- [ ] Shows fee prominently
- [ ] Status badges have correct colors
- [ ] "Accept Offer" button only on SUBMITTED offers
- [ ] Responsive on mobile

#### RazorpayCheckout Component
- [ ] Fee breakdown displays correctly
- [ ] Platform fee note shows
- [ ] "Proceed to Payment" button works
- [ ] Razorpay modal opens
- [ ] Success callback triggers
- [ ] Failure callback triggers

#### SubmitOfferForm Component
- [ ] All fields render correctly
- [ ] Validation works (required fee)
- [ ] Platform fee notice displays
- [ ] Submit button disabled while submitting
- [ ] Cancel button works

## 🧪 Webhook Testing

### Setup ngrok (for local testing)

1. **Install ngrok**: Download from https://ngrok.com/download

2. **Start ngrok**:
```bash
ngrok http 8080
```

3. **Copy the HTTPS URL** (e.g., `https://abc123.ngrok.io`)

4. **Configure Razorpay Webhook**:
   - Go to Razorpay Dashboard → Settings → Webhooks
   - Add webhook URL: `https://abc123.ngrok.io/api/payments/webhook`
   - Select events: `payment.captured`, `payment.failed`
   - Copy the webhook secret

5. **Update application.properties**:
```properties
razorpay.webhook.secret=YOUR_WEBHOOK_SECRET_HERE
```

6. **Restart backend**

### Test Webhook

1. Complete a payment in the UI
2. Check backend logs for:
```
Webhook signature verified successfully
Processing payment success for order: order_xxx
Payment status updated to SUCCESS
Case status updated to IN_PROGRESS
```

## 🔍 Debugging Checklist

### Backend Issues

**Problem**: Compilation errors
- [ ] Check all imports are correct
- [ ] Verify Razorpay dependency in pom.xml
- [ ] Run `mvn clean install`

**Problem**: 401 Unauthorized
- [ ] Check JWT token is valid
- [ ] Verify Authorization header format
- [ ] Check user has correct role (USER/LAWYER)

**Problem**: Webhook fails
- [ ] Verify webhook secret matches
- [ ] Check ngrok is running
- [ ] Verify webhook URL in Razorpay dashboard
- [ ] Check backend logs for signature errors

### Frontend Issues

**Problem**: Components not showing
- [ ] Check imports in CaseDetail.js
- [ ] Verify tab name matches ("offers")
- [ ] Check case status conditions

**Problem**: API calls fail
- [ ] Check browser console for errors
- [ ] Verify backend is running
- [ ] Check API base URL in api.js
- [ ] Verify JWT token in localStorage

**Problem**: Razorpay modal doesn't open
- [ ] Check browser console for script errors
- [ ] Verify Razorpay SDK loaded (check Network tab)
- [ ] Check Razorpay key ID is correct
- [ ] Ensure no popup blockers

## 📊 Test Scenarios

### Scenario 1: Happy Path
1. User creates case → PUBLISHED
2. Lawyer submits offer → UNDER_REVIEW
3. User accepts offer → PAYMENT_PENDING
4. User pays → IN_PROGRESS
5. Lawyer provides solution → CLOSED

**Expected**: All transitions smooth, no errors

### Scenario 2: Multiple Offers
1. User creates case
2. 3 different lawyers submit offers
3. User accepts one offer
4. **Verify**: Other 2 offers marked REJECTED

### Scenario 3: Payment Failure
1. Accept offer
2. Use test card: 4000 0000 0000 0002
3. **Verify**: 
   - Payment fails
   - Case status → PAYMENT_FAILED
   - User can retry

### Scenario 4: Offer Limit
1. Have 5 lawyers submit offers
2. 6th lawyer tries to submit
3. **Verify**: Error "Maximum offers limit reached"

### Scenario 5: Duplicate Offer
1. Lawyer submits offer
2. Same lawyer tries again
3. **Verify**: Error "Already submitted an offer"

## 📝 Manual Verification Checklist

### Database
- [ ] `offers` table has records
- [ ] `payments` table has records
- [ ] `cases` table has `selected_offer_id` populated
- [ ] Offer statuses update correctly
- [ ] Payment statuses update correctly

### Backend Logs
- [ ] No exceptions during offer submission
- [ ] Razorpay order created successfully
- [ ] Webhook signature verified
- [ ] Payment status updated

### Frontend UI
- [ ] Offers display in grid
- [ ] Payment breakdown shows correct amounts
- [ ] Status badges show correct colors
- [ ] Loading states work
- [ ] Error messages display

### Security
- [ ] Unauthorized users can't submit offers
- [ ] Users can't accept offers on others' cases
- [ ] Webhook signature verification works
- [ ] Amount validation prevents tampering

## 🎯 Success Criteria

✅ **Backend**: All API endpoints return 200 OK
✅ **Frontend**: All components render without errors
✅ **Payment**: Test payment completes successfully
✅ **Webhook**: Backend receives and processes webhook
✅ **Database**: All records created correctly
✅ **UI/UX**: Smooth user experience, no broken layouts

---

**Need Help?** Check:
1. Browser console for frontend errors
2. Backend logs for API errors
3. Network tab for failed requests
4. Database for data inconsistencies
