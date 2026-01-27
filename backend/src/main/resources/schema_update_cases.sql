-- Schema update script for Cases and Messages functionality
-- Run this script to add the new tables and columns

-- Add case_id column to client_audio table
-- Note: Remove the IF NOT EXISTS check if you get an error (MySQL doesn't support it in ALTER TABLE)
ALTER TABLE client_audio ADD COLUMN case_id BIGINT;

-- Create Cases table
CREATE TABLE IF NOT EXISTS cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    lawyer_id BIGINT,
    case_title VARCHAR(255) NOT NULL,
    case_type VARCHAR(100),
    case_status VARCHAR(50) DEFAULT 'open',
    description LONGTEXT,
    solution LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_lawyer_id (lawyer_id),
    INDEX idx_case_status (case_status)
);

-- Create Messages table
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT,
    sender_id BIGINT NOT NULL,
    sender_type VARCHAR(20) NOT NULL,
    receiver_id BIGINT NOT NULL,
    receiver_type VARCHAR(20) NOT NULL,
    message_text LONGTEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_case_id (case_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_created_at (created_at)

);

