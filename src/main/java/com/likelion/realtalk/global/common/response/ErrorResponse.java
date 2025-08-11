package com.likelion.realtalk.global.common.response;

import com.likelion.realtalk.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.BindingResult;


@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)

public class ErrorResponse {
  private String code;
  private String message;
  private List<FieldError> errors;

  private ErrorResponse(ErrorCode code, List<FieldError> fieldErrors) {
    this.code = code.getCode();
    this.message = code.getMessage();
    this.errors = fieldErrors;
  }

  private ErrorResponse(ErrorCode code) {
    this.code = code.getCode();
    this.message = code.getMessage();
    this.errors = new ArrayList<>();
  }

  public static ErrorResponse of(final ErrorCode code, final BindingResult bindingResult) {
    return new ErrorResponse(code, FieldError.of(bindingResult));
  }

  public static ErrorResponse of(ErrorCode code) {
    return new ErrorResponse(code);
  }


  @Getter
  @AllArgsConstructor
  public static class FieldError {
    private String field;
    private String value;
    private String reason;

    public static List<FieldError> of(BindingResult bindingResult) {
      return bindingResult.getFieldErrors()
          .stream()
          .map(err -> new FieldError(
              err.getField(),
              err.getRejectedValue() == null ? "" : err.getRejectedValue().toString(),
              err.getDefaultMessage()
          ))
          .collect(Collectors.toList());
    }


  }

}
