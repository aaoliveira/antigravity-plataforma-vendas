package com.sellerintelligence.modules.reporting.security;

public class ExportAccessDeniedException extends RuntimeException {
    public ExportAccessDeniedException(String message) {
        super(message);
    }
}
