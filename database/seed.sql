-- 本地课程演示账号；密码仅以 BCrypt 摘要保存，原始密码与账号同名。
INSERT INTO sys_user (id, username, password_hash, role, training_status)
VALUES
    (1, 'student01', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (2, 'student02', '$2a$10$LhFu8Z8tnuXZtZafey2TC.DwAysTnNvYvhKEfSOIJLHTwqfGr3EmW', 'STUDENT', 'FAILED'),
    (3, 'admin01', '$2a$10$yNSL6vq3gBTIV2ADoPOycuKmz4PwQ7BvM8G6irCy9XxFa.W6B7j.e', 'ADMIN', 'PENDING')
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    training_status = VALUES(training_status);
