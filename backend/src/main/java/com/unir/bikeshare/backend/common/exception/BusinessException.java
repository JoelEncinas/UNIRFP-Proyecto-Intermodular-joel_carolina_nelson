package com.unir.bikeshare.backend.common.exception;

public class BusinessException extends RuntimeException{
	public BusinessException(String message) {
        super(message);
    }
}
