package com.fipe.processor.presentation.exception;

public class BrandNotFoundException extends RuntimeException {
    public BrandNotFoundException(String message) {
        super(message);
    }
}
