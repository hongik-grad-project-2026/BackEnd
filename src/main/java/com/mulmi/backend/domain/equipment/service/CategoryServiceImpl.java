package com.mulmi.backend.domain.equipment.service;

import com.mulmi.backend.domain.equipment.converter.CategoryConverter;
import com.mulmi.backend.domain.equipment.dto.request.CategoryCreateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.request.CategoryUpdateRequestDTO;
import com.mulmi.backend.domain.equipment.dto.response.CategoryResponseDTO;
import com.mulmi.backend.domain.equipment.entity.Category;
import com.mulmi.backend.domain.equipment.exception.CategoryException;
import com.mulmi.backend.domain.equipment.exception.code.CategoryErrorCode;
import com.mulmi.backend.domain.equipment.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    // 카테고리 등록
    @Override
    @Transactional
    public CategoryResponseDTO createCategory(CategoryCreateRequestDTO dto) {
        // "  노트북  " 같이 들어오는 경우 양쪽 공백 제거 먼저
        String name = dto.name().trim();

        Optional<Category> found = categoryRepository.findByName(name);

        // 같은 이름이 아예 없으면 새로 등록한다.
        if (found.isEmpty()) {
            Category saved = categoryRepository.save(CategoryConverter.toCategory(name));
            return CategoryConverter.toCategoryResponseDTO(saved);
        }

        Category category = found.get();

        // 살아있는 카테고리가 이미 그 이름을 쓰고 있다.
        if (category.getDeletedAt() == null) {
            throw new CategoryException(CategoryErrorCode.DUPLICATE_CATEGORY_NAME);
        }

        // 삭제됐던 카테고리를 되살린다. save() 없이 더티 체킹으로 UPDATE된다.
        category.restore();
        return CategoryConverter.toCategoryResponseDTO(category);
    }

    // 디비에 저장된 카테고리 목록을 읽어서 클라한테 돌려주는 메소드
    @Override
    public List<CategoryResponseDTO> getCategories() {
        List<Category> categories = categoryRepository.findAllByDeletedAtIsNull();
        return categories.stream()
                .map(CategoryConverter::toCategoryResponseDTO)
                .toList();
    }

    // 카테고리 수정
    @Override
    @Transactional
    public CategoryResponseDTO updateCategory(Long categoryId, CategoryUpdateRequestDTO dto) {
        String name = dto.name().trim();

        // 수정 대상. 삭제된 카테고리는 쿼리에서 이미 걸러진다.
        Category category = categoryRepository.findByIdAndDeletedAtIsNull(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        // 바꾸려는 이름을 다른 카테고리가 점유하고 있으면 안 된다.
        // 삭제된 카테고리도 이름을 점유하므로 findByName으로 찾는다.
        Optional<Category> sameName = categoryRepository.findByName(name);
        if (sameName.isPresent() && !sameName.get().getId().equals(categoryId)) {
            throw new CategoryException(CategoryErrorCode.DUPLICATE_CATEGORY_NAME);
        }

        // save() 없이 더티 체킹으로 UPDATE된다.
        category.updateCategoryName(name);
        return CategoryConverter.toCategoryResponseDTO(category);
    }
}
