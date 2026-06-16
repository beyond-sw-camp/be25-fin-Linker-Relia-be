CREATE TABLE customer_status_history (
                                         id CHAR(36) NOT NULL,
                                         customer_status_sequence INT NOT NULL,
                                         customer_id CHAR(36) NOT NULL,
                                         before_status VARCHAR(30) NULL,
                                         after_status VARCHAR(30) NOT NULL,
                                         changed_reason VARCHAR(255) NULL,
                                         changed_at DATETIME NOT NULL,
                                         changed_by CHAR(36) NOT NULL,
                                         PRIMARY KEY (id),
                                         UNIQUE KEY uk_customer_status_history_customer_sequence (customer_id, customer_status_sequence),
                                         KEY idx_customer_status_history_customer_changed_at (customer_id, changed_at),
                                         CONSTRAINT chk_customer_status_history_before_status CHECK (
                                             before_status IS NULL OR before_status IN ('PROSPECT', 'CONTRACTED', 'COMPLETED', 'TERMINATED')
                                         ),
                                         CONSTRAINT chk_customer_status_history_after_status CHECK (
                                             after_status IN ('PROSPECT', 'CONTRACTED', 'COMPLETED', 'TERMINATED')
                                         ),
                                         CONSTRAINT fk_customer_status_history_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
                                         CONSTRAINT fk_customer_status_history_changed_by FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
