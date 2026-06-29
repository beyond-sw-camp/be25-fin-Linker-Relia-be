CREATE TABLE esg_impact_statistics (
    user_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    target_month VARCHAR(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    consultation_count INT NOT NULL,
    ai_briefing_count INT NOT NULL,
    handover_count INT NOT NULL,
    e_sign_count INT NOT NULL,
    paper_saved_count INT NOT NULL,
    co2_saved_kg DECIMAL(10,3) NOT NULL,
    sea_level_contribution DECIMAL(10,5) NOT NULL,
    earth_temperature_reduction DECIMAL(10,6) NOT NULL,
    level INT NOT NULL,
    recovery_rate DECIMAL(5,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_esg_impact_statistics_user_month (user_id, target_month),
    CONSTRAINT fk_esg_impact_statistics_user FOREIGN KEY (user_id) REFERENCES users(id)
);
