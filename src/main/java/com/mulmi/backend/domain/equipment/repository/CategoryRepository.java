package com.mulmi.backend.domain.equipment.repository;

import com.mulmi.backend.domain.equipment.entity.Category;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 이름으로 1건 조회. 삭제된 카테고리도 포함한다.
    // category_name이 UNIQUE라 삭제된 행도 이름을 점유하고 있기 때문.
    Optional<Category> findByName(String name);

    // 카테고리 목록 조회. 삭제된 것은 제외한다.
    List<Category> findAllByDeletedAtIsNull();

    // 대시보드 62 : 이름으로 고른 카테고리 (모델이 없어도 응답에 빈 목록으로 나가야 해서 따로 조회)
    List<Category> findAllByNameInAndDeletedAtIsNullOrderByIdAsc(List<String> names);

    // 수정·삭제 대상 조회. 삭제된 카테고리는 대상이 될 수 없다.
    Optional<Category> findByIdAndDeletedAtIsNull(Long id);

    // 접두사 없는 번호(예: 007) 발급·수정을 한 줄로 세우는 잠금
    //  접두사 없는 카테고리끼리 번호 공간을 공유하므로(label_code UNIQUE) 그 카테고리 전부를 잠근다
    //  삭제된 카테고리 포함(기기 번호는 남아 있음), id 순서 고정 → 여러 요청이 같은 순서로 잠가 교착이 없다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Category> findAllWithLockByLabelRequiredFalseOrderByIdAsc();
}
