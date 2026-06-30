CREATE TABLE esg_impact_statistics (
                                       id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,

                                       user_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                       target_month VARCHAR(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,

                                       consultation_count INT NOT NULL,
                                       ai_briefing_count INT NOT NULL,
                                       handover_count INT NOT NULL,
                                       e_sign_count INT NOT NULL,

                                       paper_saved_count INT NOT NULL,
                                       co2_saved_kg DECIMAL(10,3) NOT NULL,

                                       environmental_contribution_index DECIMAL(10,2) NOT NULL,
                                       ocean_recovery_index DECIMAL(10,2) NOT NULL,

                                       level INT NOT NULL,
                                       recovery_rate DECIMAL(5,2) NOT NULL,

                                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       created_by CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,

                                       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       updated_by CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,

                                       deleted_at DATETIME NULL,
                                       deleted_by CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,

                                       PRIMARY KEY (id),

                                       UNIQUE KEY uk_esg_impact_statistics_user_month (user_id, target_month),

                                       CONSTRAINT fk_esg_impact_statistics_user
                                           FOREIGN KEY (user_id) REFERENCES users(id),

                                       CONSTRAINT fk_esg_impact_statistics_created_by
                                           FOREIGN KEY (created_by) REFERENCES users(id),

                                       CONSTRAINT fk_esg_impact_statistics_updated_by
                                           FOREIGN KEY (updated_by) REFERENCES users(id),

                                       CONSTRAINT fk_esg_impact_statistics_deleted_by
                                           FOREIGN KEY (deleted_by) REFERENCES users(id)
);