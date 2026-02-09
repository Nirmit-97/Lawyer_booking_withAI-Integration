import React from 'react';
import { Link } from 'react-router-dom';
import './LandingPage.css';

function LandingPage() {
  return (
    <div className="landing-page">
      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-content">
          <h1 className="hero-title">LegalConnect</h1>
          <p className="hero-subtitle">Your Trusted Legal Consultation Platform</p>
          <p className="hero-description">
            Connect with experienced lawyers, book appointments, and get expert legal advice
            with privacy-protected audio processing.
          </p>
          <div className="hero-buttons">
            <Link to="/user-register" className="btn btn-primary">
              Get Started
            </Link>
            <Link to="/user-login" className="btn btn-secondary">
              Sign In
            </Link>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="features-section">
        <div className="container">
          <h2 className="section-title">Why Choose LegalConnect?</h2>
          <div className="features-grid">
            <div className="feature-card">
              <div className="feature-icon">🎤</div>
              <h3>Audio Processing</h3>
              <p>
                Record and upload your legal concerns. Our AI-powered system processes
                your audio with privacy protection, masking personal information while
                preserving case details.
              </p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">📅</div>
              <h3>Easy Booking</h3>
              <p>
                Book appointments with qualified lawyers at your convenience.
                Choose from video calls, phone consultations, or in-person meetings.
              </p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">🔒</div>
              <h3>Privacy Protected</h3>
              <p>
                Your personal information is automatically masked using advanced AI,
                ensuring your privacy while maintaining case context.
              </p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">🌐</div>
              <h3>Multi-Language</h3>
              <p>
                Support for English and Gujarati languages. Record in your preferred
                language and get translations automatically.
              </p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">⚖️</div>
              <h3>Expert Lawyers</h3>
              <p>
                Connect with verified lawyers specializing in various areas of law,
                from criminal to family law.
              </p>
            </div>
            <div className="feature-card">
              <div className="feature-icon">💬</div>
              <h3>Secure Communication</h3>
              <p>
                All communications are encrypted and secure. Your data is protected
                throughout the entire consultation process.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section className="how-it-works-section">
        <div className="container">
          <h2 className="section-title">How It Works</h2>
          <div className="steps-container">
            <div className="step">
              <div className="step-number">1</div>
              <h3>Create Account</h3>
              <p>Sign up for a free account in seconds</p>
            </div>
            <div className="step">
              <div className="step-number">2</div>
              <h3>Record or Upload</h3>
              <p>Record your legal concern or upload an audio file</p>
            </div>
            <div className="step">
              <div className="step-number">3</div>
              <h3>AI Processing</h3>
              <p>Our system processes and protects your information</p>
            </div>
            <div className="step">
              <div className="step-number">4</div>
              <h3>Book Appointment</h3>
              <p>Schedule a consultation with a qualified lawyer</p>
            </div>
            <div className="step">
              <div className="step-number">5</div>
              <h3>Get Legal Advice</h3>
              <p>Receive expert legal guidance and support</p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="cta-section">
        <div className="container">
          <h2>Ready to Get Started?</h2>
          <p>Join thousands of users who trust LegalConnect for their legal needs</p>
          <div className="cta-buttons">
            <Link to="/user-register" className="btn btn-primary btn-large">
              Create Free Account
            </Link>
            <Link to="/lawyer-login" className="btn btn-outline btn-large">
              Lawyer Login
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="landing-footer">
        <div className="container">
          <p>&copy; 2024 LegalConnect. All rights reserved.</p>
          <div className="footer-links">
            <Link to="/user-login">User Login</Link>
            <Link to="/lawyer-login">Lawyer Login</Link>
            <Link to="/lawyer-register">Lawyer Register</Link>
            <Link to="/user-register">User Register</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default LandingPage;

