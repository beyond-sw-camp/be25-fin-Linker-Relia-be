CREATE TABLE schedules (
                           id CHAR(36) NOT NULL COMMENT '일정 UUID',

                           fp_id CHAR(36) NOT NULL COMMENT '계정 UUID',
                           customer_id CHAR(36) NULL COMMENT '고객 UUID',
                           contract_id CHAR(36) NULL COMMENT '계약 UUID',
                           consultation_id CHAR(36) NULL COMMENT '상담일지 UUID',

                           schedule_type VARCHAR(30) NOT NULL COMMENT 'CONSULTATION, CONTRACT_EXPIRY, PERSONAL_MEMO',
                           title VARCHAR(100) NOT NULL COMMENT '일정 제목',
                           content VARCHAR(500) NULL COMMENT '일정 메모',

                           scheduled_at DATETIME NOT NULL COMMENT '일정 일시',
                           schedule_status VARCHAR(30) NOT NULL COMMENT 'SCHEDULED, COMPLETED, CANCELED',

                           created_at DATETIME NOT NULL,
                           created_by CHAR(36) NOT NULL,

                           updated_at DATETIME NOT NULL,
                           updated_by CHAR(36) NOT NULL,

                           deleted_at DATETIME NULL,
                           deleted_by CHAR(36) NULL,

                           PRIMARY KEY (id),

                           CONSTRAINT fk_schedules_fp
                               FOREIGN KEY (fp_id)
                                   REFERENCES users(id),

                           CONSTRAINT fk_schedules_customer
                               FOREIGN KEY (customer_id)
                                   REFERENCES customers(id),

                           CONSTRAINT fk_schedules_contract
                               FOREIGN KEY (contract_id)
                                   REFERENCES contracts(id),

                           CONSTRAINT fk_schedules_consultation
                               FOREIGN KEY (consultation_id)
                                   REFERENCES consultations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;