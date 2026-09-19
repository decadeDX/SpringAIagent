CREATE TABLE sys_user (
    id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(16) NOT NULL,
    training_status VARCHAR(16) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT ck_user_role CHECK (role IN ('STUDENT', 'ADMIN')),
    CONSTRAINT ck_user_training_status CHECK (training_status IN ('PENDING', 'PASSED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE lab (
    id VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    capacity INT NOT NULL,
    equipment_description TEXT NOT NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_lab_name (name),
    CONSTRAINT ck_lab_capacity CHECK (capacity > 0),
    CONSTRAINT ck_lab_opening_hours CHECK (open_time < close_time),
    CONSTRAINT ck_lab_status CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reservation (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    lab_id VARCHAR(32) NOT NULL,
    start_time DATETIME(3) NOT NULL,
    end_time DATETIME(3) NOT NULL,
    participant_count INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    cancelled_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_reservation_user_status_end (user_id, status, end_time),
    KEY idx_reservation_lab_start (lab_id, start_time),
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_reservation_lab FOREIGN KEY (lab_id) REFERENCES lab (id),
    CONSTRAINT ck_reservation_time CHECK (start_time < end_time),
    CONSTRAINT ck_reservation_participants CHECK (participant_count > 0),
    CONSTRAINT ck_reservation_status CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_reservation_cancellation CHECK (
        (status = 'CONFIRMED' AND cancelled_at IS NULL)
        OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reservation_slot (
    id BIGINT NOT NULL,
    lab_id VARCHAR(32) NOT NULL,
    slot_start_time DATETIME(3) NOT NULL,
    reservation_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_lab_slot UNIQUE (lab_id, slot_start_time),
    KEY idx_slot_reservation (reservation_id),
    CONSTRAINT fk_slot_lab FOREIGN KEY (lab_id) REFERENCES lab (id),
    CONSTRAINT fk_slot_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE repair_ticket (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    lab_id VARCHAR(32) NOT NULL,
    equipment_info VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    safety_risk BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(16) NOT NULL,
    resolution_note TEXT NULL,
    processed_by BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_ticket_user_created (user_id, created_at),
    KEY idx_ticket_status_created (status, created_at),
    CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_ticket_lab FOREIGN KEY (lab_id) REFERENCES lab (id),
    CONSTRAINT fk_ticket_processor FOREIGN KEY (processed_by) REFERENCES sys_user (id),
    CONSTRAINT ck_ticket_status CHECK (status IN ('SUBMITTED', 'PROCESSING', 'RESOLVED')),
    CONSTRAINT ck_ticket_resolution CHECK (
        status = 'SUBMITTED' OR (resolution_note IS NOT NULL AND CHAR_LENGTH(TRIM(resolution_note)) > 0)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE knowledge_document (
    id BIGINT NOT NULL,
    logical_document_code VARCHAR(64) NOT NULL,
    version VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    effective_at DATETIME(3) NOT NULL,
    applicable_lab_ids JSON NULL,
    publish_status VARCHAR(16) NOT NULL,
    index_status VARCHAR(16) NOT NULL,
    source_file_path VARCHAR(512) NOT NULL,
    index_failure_reason VARCHAR(1000) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_document_version UNIQUE (logical_document_code, version),
    KEY idx_document_retrieval (publish_status, index_status),
    CONSTRAINT fk_document_creator FOREIGN KEY (created_by) REFERENCES sys_user (id),
    CONSTRAINT ck_document_publish_status CHECK (publish_status IN ('DRAFT', 'PUBLISHED', 'DISABLED')),
    CONSTRAINT ck_document_index_status CHECK (index_status IN ('PENDING', 'INDEXING', 'SUCCEEDED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE knowledge_chunk (
    id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    content MEDIUMTEXT NOT NULL,
    summary TEXT NULL,
    vector_record_id VARCHAR(255) NOT NULL,
    content_hash CHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_chunk_sequence UNIQUE (document_id, sequence_no),
    KEY idx_chunk_document (document_id),
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES knowledge_document (id),
    CONSTRAINT ck_chunk_sequence CHECK (sequence_no > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE action_execution (
    action_id CHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    execution_status VARCHAR(16) NOT NULL,
    payload JSON NOT NULL,
    business_id BIGINT NULL,
    result_summary JSON NULL,
    failure_code VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (action_id),
    KEY idx_action_user (user_id),
    CONSTRAINT fk_action_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT ck_action_type CHECK (action_type IN ('CREATE_RESERVATION', 'CANCEL_RESERVATION', 'CREATE_REPAIR_TICKET')),
    CONSTRAINT ck_action_status CHECK (execution_status IN ('EXECUTING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_action_outcome CHECK (
        (execution_status = 'SUCCEEDED' AND business_id IS NOT NULL AND result_summary IS NOT NULL AND failure_code IS NULL)
        OR (execution_status = 'FAILED' AND failure_code IS NOT NULL)
        OR execution_status = 'EXECUTING'
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE agent_trace (
    id BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NULL,
    user_id BIGINT NULL,
    tool_name VARCHAR(128) NOT NULL,
    redacted_arguments JSON NULL,
    result_summary VARCHAR(1000) NULL,
    duration_ms INT NOT NULL,
    error_code VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_trace_request (request_id),
    KEY idx_trace_user_session (user_id, session_id),
    CONSTRAINT fk_trace_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT ck_trace_duration CHECK (duration_ms >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
