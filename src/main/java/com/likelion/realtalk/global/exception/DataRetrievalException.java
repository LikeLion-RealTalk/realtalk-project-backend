package com.likelion.realtalk.global.exception;

public class DataRetrievalException extends CustomException {

  public DataRetrievalException(ErrorCode errorCode) {
    super(errorCode);
  }
}
