package com.likelion.realtalk.domain.category.api;

import com.likelion.realtalk.domain.category.dto.response.CategoryResponse;
import com.likelion.realtalk.domain.category.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

  private final CategoryService categoryService;

  /**
   * 카테고리 전체조회 api 입니다.
   *
   * @return 카테고리 전체 조회 결과를 CategoryResponse 리스트로 반환합니다.
   */
  @GetMapping("/all")
  public ResponseEntity<List<CategoryResponse>> getAllCategories() {
    List<CategoryResponse> categories = categoryService.getAllCategories();
    return ResponseEntity.ok(categories);
  }




}
