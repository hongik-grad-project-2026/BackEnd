-- 공지사항 목록 조회수 표시를 위한 컬럼이다.
-- 새 애플리케이션 버전을 배포하기 전에 RDS의 mulmi DB에서 한 번만 실행한다.
ALTER TABLE notices
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0 AFTER important;

SHOW CREATE TABLE notices;
