USE login_app;
INSERT INTO users (username, password, nic_number, failed_login_attempts, account_locked, created_at) 
VALUES ('dumindu', '123Asd@123', '200111101179', 0, 0, NOW());
SELECT * FROM users;
