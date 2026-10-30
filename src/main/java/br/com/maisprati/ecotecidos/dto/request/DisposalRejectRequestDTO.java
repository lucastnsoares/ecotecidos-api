package br.com.maisprati.ecotecidos.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DisposalRejectRequestDTO(
        @NotBlank(message = "A justificativa é obrigatória ao rejeitar um descarte.")
        String justification
) {}