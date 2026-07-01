INSERT INTO organization_monthly_closing (
    id,
    closing_month,
    organization_id,
    organization_code,
    organization_name,
    organization_type,
    organization_status,
    closed_at
)
SELECT
    UUID(),
    '2026-06',
    o.id,
    o.organization_code,
    o.organization_name,
    o.organization_type,
    o.organization_status,
    TIMESTAMP('2026-06-30', '23:59:59')
FROM organizations o
WHERE o.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1
    FROM organization_monthly_closing omc
    WHERE omc.closing_month = '2026-06'
  AND omc.organization_id = o.id
    );