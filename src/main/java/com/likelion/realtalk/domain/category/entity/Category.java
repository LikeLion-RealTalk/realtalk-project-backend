package com.likelion.realtalk.domain.category.entity;

import com.likelion.realtalk.domain.category.dto.response.CategoryResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

@Getter
@Entity
public class Category {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "category_id")
  private Long id;

  @Column(name = "category_name")
  private String categoryName;

  public CategoryResponse toResponse() {
    return CategoryResponse.builder()
        .id(this.getId())
        .name(this.getCategoryName())
        .build();
  }
}