package br.com.sellerintelligence.discovery.web;

import jakarta.validation.constraints.NotBlank;

public record UploadCatalogRequest(
        @NotBlank(message = "Nome do arquivo é obrigatório")
        String filename,

        @NotBlank(message = "Conteúdo do arquivo (Base64) é obrigatório")
        String fileContentBase64,

        String supplierName
) {}
