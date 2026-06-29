ALTER TABLE handover_recommendations
    ADD COLUMN matching_reasons_json TEXT NULL AFTER recommendation_reason;
