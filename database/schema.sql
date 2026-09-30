-- Organizational Communication System - database schema (MySQL 8+)
CREATE DATABASE IF NOT EXISTS ocs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ocs;

CREATE TABLE IF NOT EXISTS users (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  name            VARCHAR(100) NOT NULL,
  username        VARCHAR(50)  NOT NULL UNIQUE,
  password_hash   VARCHAR(255) NOT NULL,
  role            ENUM('employee','manager','admin') NOT NULL DEFAULT 'employee',
  status          ENUM('pending','active','suspended','rejected','deactivated','locked') NOT NULL DEFAULT 'pending',
  department      VARCHAR(100) NOT NULL DEFAULT '',
  failed_attempts INT NOT NULL DEFAULT 0,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS messages (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  sender_id    INT NOT NULL,
  recipient_id INT NOT NULL,
  type         ENUM('feedback','complaint','request','general') NOT NULL DEFAULT 'general',
  body         TEXT NOT NULL,
  anonymous    BOOLEAN NOT NULL DEFAULT FALSE,
  sent_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (sender_id)    REFERENCES users(id),
  FOREIGN KEY (recipient_id) REFERENCES users(id),
  INDEX idx_msg_recipient (recipient_id),
  INDEX idx_msg_sender (sender_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS calendar_events (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(200) NOT NULL,
  event_date  DATE NOT NULL,
  description TEXT,
  created_by  INT NULL,
  FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS announcements (
  id        INT AUTO_INCREMENT PRIMARY KEY,
  title     VARCHAR(200) NOT NULL,
  body      TEXT NOT NULL,
  created_by INT NULL,
  posted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS polls (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  question   VARCHAR(300) NOT NULL,
  created_by INT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS poll_options (
  id      INT AUTO_INCREMENT PRIMARY KEY,
  poll_id INT NOT NULL,
  label   VARCHAR(200) NOT NULL,
  FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Primary key (poll_id, user_id) enforces BR-5: one vote per user per poll
CREATE TABLE IF NOT EXISTS poll_votes (
  poll_id   INT NOT NULL,
  user_id   INT NOT NULL,
  option_id INT NOT NULL,
  voted_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (poll_id, user_id),
  FOREIGN KEY (poll_id)   REFERENCES polls(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id)   REFERENCES users(id),
  FOREIGN KEY (option_id) REFERENCES poll_options(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS meetings (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  title        VARCHAR(200) NOT NULL,
  meeting_time DATETIME NOT NULL,
  invitees     VARCHAR(500) NOT NULL DEFAULT '',
  created_by   INT NOT NULL,
  FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX idx_meeting_time (meeting_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS violations (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  reporter_id INT NOT NULL,
  description TEXT NOT NULL,
  status      ENUM('open','resolved') NOT NULL DEFAULT 'open',
  reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (reporter_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS login_history (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  user_id    INT NOT NULL,
  login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip_address VARCHAR(64) NOT NULL DEFAULT '',
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_login_user (user_id)
) ENGINE=InnoDB;

-- Audit trail of administrator actions (SR-5)
CREATE TABLE IF NOT EXISTS audit_log (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  admin_id   INT NOT NULL,
  action     VARCHAR(60) NOT NULL,
  details    VARCHAR(300) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (admin_id) REFERENCES users(id)
) ENGINE=InnoDB;
