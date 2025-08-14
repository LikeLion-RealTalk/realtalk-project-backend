package com.likelion.realtalk.domain.category.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CategoryResponse {

  private Long id;
  private String name;

}
