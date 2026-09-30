-- Optional sample data. Run AFTER schema.sql.
-- The first administrator is NOT created here; it is created on first start
-- from admin.* in config.properties (password is stored hashed).
USE ocs;

INSERT INTO calendar_events (title, event_date, description) VALUES
  ('Quarterly Review Meeting', DATE_ADD(CURDATE(), INTERVAL 7 DAY),  'Second conference hall, 10:00 AM'),
  ('Company Sports Day',       DATE_ADD(CURDATE(), INTERVAL 21 DAY), 'Main ground');

INSERT INTO announcements (title, body) VALUES
  ('Welcome to OrgComm', 'Use this portal for feedback, requests, announcements, polls and meeting schedules.');

INSERT INTO polls (question) VALUES ('Preferred time for the monthly all-hands meeting?');
SET @poll_id = LAST_INSERT_ID();
INSERT INTO poll_options (poll_id, label) VALUES (@poll_id, 'Morning'), (@poll_id, 'Afternoon');
