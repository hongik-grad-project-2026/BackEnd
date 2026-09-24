package com.mulmi.backend.domain.equipment.repository;

import com.mulmi.backend.domain.equipment.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 이름으로 1건 조회. 삭제된 카테고리도 포함한다.
    // category_name이 UNIQUE라 삭제된 행도 이름을 점유하고 있기 때문.
    Optional<Category> findByName(String name);

    // 카테고리 목록 조회. 삭제된 것은 제외한다.
    List<Category> findAllByDeletedAtIsNull();

    // 수정·삭제 대상 조회. 삭제된 카테고리는 대상이 될 수 없다.
    Optional<Category> findByIdAndDeletedAtIsNull(Long id);
}
