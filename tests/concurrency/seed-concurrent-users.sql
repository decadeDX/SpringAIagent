-- 并发验收专用的二十位已培训学生。所有账号使用密码 student01；仅用于本地测试环境。
INSERT INTO sys_user (id, username, password_hash, role, training_status)
VALUES
    (1001, 'concurrency01', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1002, 'concurrency02', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1003, 'concurrency03', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1004, 'concurrency04', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1005, 'concurrency05', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1006, 'concurrency06', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1007, 'concurrency07', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1008, 'concurrency08', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1009, 'concurrency09', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1010, 'concurrency10', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1011, 'concurrency11', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1012, 'concurrency12', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1013, 'concurrency13', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1014, 'concurrency14', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1015, 'concurrency15', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1016, 'concurrency16', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1017, 'concurrency17', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1018, 'concurrency18', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1019, 'concurrency19', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED'),
    (1020, 'concurrency20', '$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi', 'STUDENT', 'PASSED')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), role = VALUES(role), training_status = VALUES(training_status);
