ALTER TABLE consultation_cancel_details
    ADD COLUMN IF NOT EXISTS reason_detail VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS customer_intent VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS result VARCHAR(100) NULL;

CREATE TABLE IF NOT EXISTS consultation_cancel_review_reasons (
    consultation_cancel_detail_id CHAR(36) NOT NULL,
    reason_order INT NOT NULL,
    review_reason VARCHAR(100) NOT NULL,
    PRIMARY KEY (consultation_cancel_detail_id, reason_order),
    CONSTRAINT fk_cancel_review_reasons_detail
        FOREIGN KEY (consultation_cancel_detail_id)
        REFERENCES consultation_cancel_details(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS consultation_cancel_retention_plans (
    consultation_cancel_detail_id CHAR(36) NOT NULL,
    plan_order INT NOT NULL,
    retention_plan VARCHAR(100) NOT NULL,
    PRIMARY KEY (consultation_cancel_detail_id, plan_order),
    CONSTRAINT fk_cancel_retention_plans_detail
        FOREIGN KEY (consultation_cancel_detail_id)
        REFERENCES consultation_cancel_details(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS consultation_cancel_next_actions (
    consultation_cancel_detail_id CHAR(36) NOT NULL,
    action_order INT NOT NULL,
    next_action VARCHAR(500) NOT NULL,
    PRIMARY KEY (consultation_cancel_detail_id, action_order),
    CONSTRAINT fk_cancel_next_actions_detail
        FOREIGN KEY (consultation_cancel_detail_id)
        REFERENCES consultation_cancel_details(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
