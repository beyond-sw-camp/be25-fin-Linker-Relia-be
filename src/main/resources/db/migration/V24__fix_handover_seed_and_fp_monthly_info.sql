-- ============================================================================
-- V24: 인수인계 테이블 전체 초기화 + fp_monthly_info 데이터 수정
--
-- 1. handover 관련 테이블 전체 DELETE
--    - V2(82*/81*/80*)의 깨진 데이터 + V19(89*)의 재구축 데이터 전부 제거
--    - 실제 기능으로 인수인계 진행 시 새 데이터가 쌓이는 구조로 전환
--    - customer_fp_history도 전체 삭제 (handover_success_count=0 허용)
--
-- 2. fp_monthly_info 버그 패치 (V15/V16에서 잘못 시드된 값 수정)
--    - preferred_customer_age: 1의 자리 값 → 10세 단위 버킷화
--    - preferred_customer_asset_level: 'MIDDLE' → 'MID'
-- ============================================================================

-- ============================================================================
-- 1. handover 관련 테이블 전체 초기화 (FK 역순)
-- ============================================================================

DELETE FROM customer_fp_history;
DELETE FROM handover_recommendations;
DELETE FROM handover_requests;

-- ============================================================================
-- 2. fp_monthly_info 버그 패치
-- ============================================================================

-- preferred_customer_age: 1의 자리 → 10세 단위 버킷화 (예: 43→40, 47→40)
UPDATE fp_monthly_info
SET preferred_customer_age = FLOOR(preferred_customer_age / 10) * 10
WHERE preferred_customer_age IS NOT NULL
  AND preferred_customer_age != FLOOR(preferred_customer_age / 10) * 10;

-- handover_success_count: history 전체 삭제했으므로 0으로 초기화
UPDATE fp_monthly_info
SET handover_success_count = 0
WHERE handover_success_count != 0;

