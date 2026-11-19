package com.sellerintelligence.modules.reporting.security;

public class ExportNotFoundException extends RuntimeException {
    public ExportNotFoundException(String message) {
        super(message);
    }
}
