# Online Lawyer Booking System with AI Integration

## 🚀 Project Overview
This is a modern, production-ready legal consultation platform that connects clients with lawyers seamlessly. The system integrates **AI-driven audio processing**, **real-time communication**, and **secure digital payments** to provide an end-to-end legal service experience.

---

## ✨ Key Features

### 👤 For Clients (Users)
- **AI Audio Consultation**: Upload audio recordings for case summaries and automated legal assistance.
- **Lawyer Discovery**: Search and preview lawyer profiles with ratings and specialties.
- **Secure Payments**: Integrated **Razorpay** checkout for consultation fees.
- **Case Management**: Track the status of legal cases from draft to resolution.
- **Real-time Messaging**: Chat directly with assigned lawyers within the platform.

### ⚖️ For Lawyers
- **Professional Dashboard**: Manage active cases, submitted offers, and upcoming appointments.
- **Offer System**: Submit professional offers to unassigned cases.
- **Case Handover**: Access detailed case files and client communication channels.
- **Profile Management**: Update professional credentials and specialties.

### 🛠️ For Administrators
- **Full Control Panel**: Monitor all users, lawyers, and legal cases.
- **System Oversight**: Oversee payments, registrations, and platform-wide activity.
- **Verification**: Role-based access control for managing lawyer approvals.

---

## 💻 Technical Stack

### Backend
- **Framework**: Spring Boot (Java)
- **Security**: Spring Security with JWT & WebSocket Security
- **Data**: JPA / Hibernate (MySQL/PostgreSQL compatible)
- **Payments**: Razorpay SDK
- **AI Integration**: Custom audio processing services

### Frontend
- **Framework**: React.js
- **State Management**: React Context API & Hooks
- **Styling**: Modern Vanilla CSS with CSS Variables
- **Icons**: React Icons (Lucide/FontAwesome)
- **Communication**: WebSockets (STOMP) for real-time chat

---

## 🔄 System Workflow (End-to-End)

1.  **Registration**: Users and Lawyers sign up with dedicated roles.
2.  **Case Creation**: Clients create a case by uploading audio or text. AI processes the initial data.
3.  **Offer Phase**: Registered lawyers view unassigned cases and submit consultation offers.
4.  **Payment**: The client accepts an offer and completes the payment via Razorpay.
5.  **Collaboration**: Once paid, a private secure channel (Chat) is opened between the client and lawyer.
6.  **Resolution**: The lawyer provides legal solutions, and the case is closed.

---

## 📂 Project Structure

```text
lawyer-booking/
├── backend/               # Spring Boot Application
│   ├── src/main/java/     # Source Code (Controllers, Services, Entities)
│   └── src/main/resources/# Configuration & SQL scripts
├── frontend/              # React.js Application
│   ├── src/components/    # UI Components (Dashboards, Login, Payments)
│   ├── src/context/       # Auth & State Management
│   └── src/utils/         # API Interceptors & Helpers
└── md files/              # Database scripts and development logs
```

---

## 🛠️ Setup Instructions

### Prerequisites
- JDK 17+
- Node.js 18+
- Maven 3.8+
- MySQL Server

### Quick Start
1.  **Database**: Run the SQL scripts in `md files/create_tables.sql`.
2.  **Backend**: 
    - Configure `application.properties` with your DB and Razorpay credentials.
    - Run `mvn spring-boot:run` in the `backend` folder.
3.  **Frontend**:
    - Run `npm install`.
    - Run `npm start` in the `frontend` folder.

---

## 🛡️ Security & Performance
- **Authorization**: Role-Based Access Control (RBAC) across all endpoints.
- **Rate Limiting**: Integrated service to prevent abuse.
- **Audit Logs**: Comprehensive tracking of major system events.

---

*This project is built with excellence in mind, focusing on scalability and user experience.*
