package com.likelion.realtalk.global.exception;

public class UserNotFoundException extends CustomException {
  public UserNotFoundException() {
    super(ErrorCode.USER_NOT_FOUND);
  }

}
