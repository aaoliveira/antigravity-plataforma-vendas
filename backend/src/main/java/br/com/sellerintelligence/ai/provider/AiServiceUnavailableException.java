package br.com.sellerintelligence.ai.provider;

import br.com.sellerintelligence.infrastructure.error.BusinessException;
import org.springframework.http.HttpStatus;

public class AiServiceUnavailableException extends BusinessException {

    public AiServiceUnavailableException(String message) {
        super("AI_SERVICE_UNAVAILABLE", message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super("AI_SERVICE_UNAVAILABLE", message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
