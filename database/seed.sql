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

INSERT INTO lab (id, name, capacity, equipment_description, open_time, close_time, status)
VALUES
    ('LAB-A301', '软件工程实验室', 30, '普通开发工作站', '08:00:00', '22:00:00', 'ACTIVE'),
    ('LAB-B402', '人工智能实验室', 12, 'GPU 工作站', '09:00:00', '21:00:00', 'ACTIVE'),
    ('LAB-C205', '嵌入式实验室', 20, '开发板、示波器', '09:00:00', '18:00:00', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    capacity = VALUES(capacity),
    equipment_description = VALUES(equipment_description),
    open_time = VALUES(open_time),
    close_time = VALUES(close_time),
    status = VALUES(status);
