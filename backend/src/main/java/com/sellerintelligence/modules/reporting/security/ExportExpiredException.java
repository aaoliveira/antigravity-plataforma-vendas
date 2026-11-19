package com.sellerintelligence.modules.reporting.security;

public class ExportExpiredException extends RuntimeException {
    public ExportExpiredException(String message) {
        super(message);
    }
}
