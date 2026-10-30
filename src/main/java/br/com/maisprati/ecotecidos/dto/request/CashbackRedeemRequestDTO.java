package br.com.maisprati.ecotecidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CashbackRedeemRequestDTO(
        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor do resgate deve ser maior que zero.")
        @Schema(description = "Valor de pontos a resgatar", example = "50.00")
        BigDecimal amount,

        @Schema(description = "Descrição ou motivo do resgate", example = "Desconto na Loja Parceira X")
        String description,

        @Schema(description = "Nome da loja parceira onde o resgate é efetuado", example = "EcoStore")
        String partnerStore
) {}