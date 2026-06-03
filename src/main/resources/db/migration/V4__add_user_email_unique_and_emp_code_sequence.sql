ALTER TABLE users
    ADD CONSTRAINT uk_users_email UNIQUE (email);

CREATE TABLE user_emp_code_sequences (
    user_role VARCHAR(30) NOT NULL,
    next_value BIGINT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO user_emp_code_sequences (user_role, next_value)
VALUES ('FP', 21);
