package br.com.maisprati.ecotecidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record DisposalCreateRequestDTO(
    @NotNull(message = "O ponto de coleta é obrigatório.")
    @Schema(description = "ID do ponto de coleta onde o tecido foi descartado", example = "1")
    Long collectionPointId,

    @NotNull(message = "O peso aproximado é obrigatório.")
    @Positive(message = "O peso deve ser maior que zero.")
    @Schema(description = "Peso aproximado do tecido descartado em kg", example = "2.5")
    BigDecimal approximateWeightInKg,

    @NotEmpty(message = "Informe ao menos um tipo de tecido que está sendo descartado.")
    @Schema(description = "IDs dos tipos de tecidos descartados", example = "[1, 2]")
    List<Long> clothTypeIds
) {
}
