package com.likelion.realtalk.domain.category.service;

import com.likelion.realtalk.domain.category.dto.response.CategoryResponse;
import com.likelion.realtalk.domain.category.entity.Category;
import com.likelion.realtalk.domain.category.repository.CategoryRepository;
import com.likelion.realtalk.global.exception.DataRetrievalException;
import com.likelion.realtalk.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {

  private final CategoryRepository categoryRepository;

  /**
   * 카테고리 전체조회 api 입니다.
   *
   * @return 카테고리 전체 조회 결과를 CategoryResponse 리스트로 반환값을 반환합니다.
   */
  @Transactional(readOnly = true)
  public List<CategoryResponse> getAllCategories() {
      try {
        List<Category> categories = categoryRepository.findAll();

        return categories.stream()
            .map(Category::toResponse)
            .toList();
      } catch (Exception e) {
        throw new DataRetrievalException(ErrorCode.DATA_RETRIEVAL_FAILED);
      }
  }

}
