package com.sellerintelligence.core;

import com.sellerintelligence.core.exception.BusinessRuleException;
import com.sellerintelligence.core.exception.GlobalExceptionHandler;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Deve formatar ResourceNotFoundException em ProblemDetail 404")
    void shouldFormatResourceNotFoundException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test/resource");

        ProblemDetail problem = handler.handleResourceNotFoundException(
                new ResourceNotFoundException("Produto não encontrado"), request
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Recurso Não Encontrado");
        assertThat(problem.getDetail()).isEqualTo("Produto não encontrado");
        assertThat(problem.getInstance().toString()).isEqualTo("/api/test/resource");
    }

    @Test
    @DisplayName("Deve formatar BusinessRuleException em ProblemDetail 422")
    void shouldFormatBusinessRuleException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test/business");

        ProblemDetail problem = handler.handleBusinessRuleException(
                new BusinessRuleException("Margem de lucro não pode ser negativa"), request
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getTitle()).isEqualTo("Violação de Regra de Negócio");
        assertThat(problem.getDetail()).isEqualTo("Margem de lucro não pode ser negativa");
    }

    @Test
    @DisplayName("Deve formatar Exception genérica em ProblemDetail 500 sem vazar detalhes sensíveis")
    void shouldFormatGenericException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test/generic");

        ProblemDetail problem = handler.handleGenericException(
                new RuntimeException("Falha de conexão com banco de dados"), request
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Erro Interno do Servidor");
        assertThat(problem.getDetail()).doesNotContain("Falha de conexão");
        assertThat(problem.getDetail()).contains("Ocorreu um erro interno inesperado");
    }
}
