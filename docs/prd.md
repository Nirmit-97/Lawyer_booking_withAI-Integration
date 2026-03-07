# Product Requirements Document (PRD): LegalConnect

## 1. Project Overview
**LegalConnect** is a modern, AI-integrated platform designed to simplify the process of legal consultation. It bridges the gap between individuals seeking legal advice and qualified lawyers through a secure, efficient, and AI-enhanced ecosystem.

The platform distinguishes itself by using **AI-driven audio processing** to handle the initial intake of case details, making it accessible for users who prefer speaking over typing.

---

## 2. Core Objectives
- **Accessibility**: Allow users to describe legal issues via voice, lowering the barrier to entry.
- **Efficiency**: Automate case classification and summarization to save lawyers' time.
- **Security**: Protect user privacy through AI-driven PII masking and secure document storage.
- **Transparency**: Provide a clear timeline of case progress and a competitive offer-based booking system.
- **Reliability**: Ensure secure payments via trusted gateways (Razorpay).

---

## 3. User Personas

### 3.1 Clients (Users)
- **Goal**: Find the right legal expert quickly and securely.
- **Key Actions**: Record/upload case audio, view lawyer profiles, accept offers, pay consultation fees, and chat with assigned lawyers.

### 3.2 Lawyers
- **Goal**: Discover new cases and manage client relationships efficiently.
- **Key Actions**: Browse "Discovery Pool" for unassigned cases, submit consultation offers, manage active cases, and securely communicate with clients.

### 3.3 Administrators
- **Goal**: Maintain platform integrity and oversee operations.
- **Key Actions**: Approve/verify lawyers, monitor transactions, view audit logs, and manage system users.

---

## 4. Functional Requirements

### 4.1 AI-Driven Audio Intake Pipeline
The platform features a multi-stage AI pipeline for processing client intake:
- **Transcription**: Converging voice to text using OpenAI Whisper (English translation).
- **Gender Detection**: Analyzing audio to identify the speaker's gender for personalized service.
- **PII Masking**: Automatically identifying and "bleeping" or masking Personal Identifiable Information (Names, Phone Numbers, Addresses) from descriptions to protect privacy.
- **Multilingual Support**: Translating case descriptions into regional languages (e.g., Gujarati) to support diverse demographics.
- **Text-to-Speech (TTS)**: Generating high-quality audio summaries for lawyers to "listen" to cases on the go.
- **Automated Classification**: AI-generated titles and category assignment (e.g., Criminal, Civil, Corporate) based on the case description.

### 4.2 Lawyer Discovery & Booking
- **Discovery Pool**: A centralized feed for lawyers to find new cases matching their specializations.
- **Bidding/Offer System**: Lawyers submit professional offers (fees and terms) for user cases.
- **Acceptance Flow**: Users review multiple offers and select the most suitable lawyer.

### 4.3 Payment & Transaction Management
- **Razorpay Integration**: Secure payment processing for consultation fees.
- **Escrow-like Workflow**: Payments are tied to case milestones (Offer Acceptance -> Case Activation).
- **Transaction History**: Comprehensive records of all payments for users and lawyers.

### 4.4 Communication & Collaboration
- **Real-time Chat**: Secure, WebSocket-based messaging between clients and their assigned lawyers.
- **Notifications**: Instant alerts for new offers, messages, and status changes.
- **Document Vault**: A secure repository for uploading and sharing legal documents (PDFs, Images, etc.) with role-based access control.

### 4.5 Case Management
- **Interactive Timeline**: A visual audit log tracking every stage of the case (Created -> Offer Accepted -> In Progress -> Closed).
- **Status Tracking**: Clear indicators of case state (Draft, Published, Under Review, In Progress, etc.).

---

## 5. Technical Stack

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.x (Java 17)
- **Security**: JWT-based Authentication, Role-Based Access Control (RBAC).
- **AI Services**: Integration with OpenAI API (Whisper, GPT-4 for classification, TTS).
- **Real-time**: Spring WebSockets (STOMP).
- **Database**: JPA/Hibernate with MySQL support.

### Frontend (React)
- **Framework**: React.js with Context API for state management.
- **Styling**: Vanilla CSS with modern design principles (Gradients, Glassmorphism).
- **Communication**: SockJS and StompJS for real-time interaction.

---

## 6. Future Roadmap
- **AI Document Analysis**: Summarizing uploaded legal documents.
- **Video Consultation**: Integrated video conferencing for face-to-face meetings.
- **Mobile Application**: Native iOS and Android apps for better mobility.
- **AI Lawyer Matching**: Proactive recommendations of the best-suited lawyers based on case complexity and lawyer success rates.
