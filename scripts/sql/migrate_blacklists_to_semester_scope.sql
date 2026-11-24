-- 블랙리스트를 날짜 범위 방식에서 학기 단위 대여 제한 방식으로 전환한다.
-- 적용 대상: 기존 blacklists(start_date, end_date, reason) 테이블
-- 주의: 애플리케이션 새 버전을 배포하기 전에 RDS의 mulmi DB에서 한 번만 실행한다.

-- 1. 새 코드가 사용하는 컬럼을 먼저 nullable로 추가해 기존 행을 보존한다.
ALTER TABLE blacklists
    ADD COLUMN semester_id BIGINT NULL AFTER user_id,
    ADD COLUMN reason_type VARCHAR(30) NULL AFTER rental_id,
    ADD COLUMN reason_detail TEXT NULL AFTER reason_type,
    ADD COLUMN registered_by BIGINT NULL AFTER reason_detail,
    ADD COLUMN released_at DATETIME(6) NULL AFTER registered_by,
    ADD COLUMN released_by BIGINT NULL AFTER released_at,
    ADD COLUMN release_reason TEXT NULL AFTER released_by;

-- 2. 기존 기록은 수동 등록 이력으로 옮긴다.
--    start_date보다 늦지 않은 학기 중 가장 최근 학기를 제재 학기로 사용한다.
--    이미 삭제됐거나 종료일이 지난 기록은 해제 이력으로 옮기고,
--    아직 종료되지 않은 기록만 해당 학기의 활성 제재로 유지한다.
UPDATE blacklists b
SET b.semester_id = (
        SELECT s.semester_id
        FROM semester s
        WHERE s.start_date <= b.start_date
        ORDER BY s.start_date DESC
        LIMIT 1
    ),
    b.reason_type = 'MANUAL',
    b.reason_detail = b.reason,
    b.released_at = CASE
        WHEN b.deleted_at IS NOT NULL THEN b.deleted_at
        WHEN b.end_date < CURRENT_DATE THEN CAST(CONCAT(b.end_date, ' 23:59:59.999999') AS DATETIME(6))
        ELSE NULL
    END,
    b.release_reason = CASE
        WHEN b.deleted_at IS NOT NULL THEN '기존 소프트 삭제 이력 마이그레이션'
        WHEN b.end_date < CURRENT_DATE THEN '기존 제재 기간 만료 이력 마이그레이션'
        ELSE NULL
    END;

-- 이 조회 결과가 0이어야 다음 ALTER가 성공한다.
-- 결과가 있다면 해당 기록에 맞는 semester_id를 먼저 직접 지정한다.
SELECT id, user_id, start_date, end_date, reason
FROM blacklists
WHERE semester_id IS NULL OR reason_type IS NULL;

-- 3. 필수 컬럼·조회 인덱스·중복 방지 제약과 외래 키를 적용한다.
ALTER TABLE blacklists
    MODIFY COLUMN semester_id BIGINT NOT NULL,
    MODIFY COLUMN reason_type VARCHAR(30) NOT NULL,
    DROP INDEX idx_blacklists_user_period,
    ADD INDEX idx_blacklists_user_semester_release (user_id, semester_id, released_at),
    ADD CONSTRAINT uk_blacklists_rental_reason UNIQUE (rental_id, reason_type),
    ADD CONSTRAINT fk_blacklists_semester
        FOREIGN KEY (semester_id) REFERENCES semester (semester_id),
    ADD CONSTRAINT fk_blacklists_rental
        FOREIGN KEY (rental_id) REFERENCES rental (rental_id),
    ADD CONSTRAINT fk_blacklists_registered_by
        FOREIGN KEY (registered_by) REFERENCES users (id),
    ADD CONSTRAINT fk_blacklists_released_by
        FOREIGN KEY (released_by) REFERENCES users (id);

-- 4. 더 이상 쓰지 않는 날짜 범위 컬럼을 제거한다.
ALTER TABLE blacklists
    DROP COLUMN start_date,
    DROP COLUMN end_date,
    DROP COLUMN reason;

-- 적용 확인
SHOW CREATE TABLE blacklists;
