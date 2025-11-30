package com.fipe.processor.domain;

public class MessageProcessingException extends RuntimeException {
    private final boolean retryable;

    public MessageProcessingException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
