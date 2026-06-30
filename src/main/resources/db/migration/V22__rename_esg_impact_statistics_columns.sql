ALTER TABLE esg_impact_statistics
    CHANGE COLUMN environmental_contribution_index sea_level_contribution DECIMAL(10,5) NOT NULL,
    CHANGE COLUMN ocean_recovery_index earth_temperature_reduction DECIMAL(10,6) NOT NULL;
