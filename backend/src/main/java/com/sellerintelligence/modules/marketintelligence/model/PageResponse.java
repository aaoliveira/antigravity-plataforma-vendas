package com.sellerintelligence.modules.marketintelligence.model;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Resposta paginada genérica para o módulo de Market Intelligence.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasMore
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
