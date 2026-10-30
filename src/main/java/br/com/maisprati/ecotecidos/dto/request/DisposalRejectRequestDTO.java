package br.com.maisprati.ecotecidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DisposalRejectRequestDTO(
        @NotBlank(message = "A justificativa é obrigatória ao rejeitar um descarte.")
        @Schema(description = "Justificativa da rejeição do descarte", example = "Material não correspondente ao declarado.")
        String justification
) {}