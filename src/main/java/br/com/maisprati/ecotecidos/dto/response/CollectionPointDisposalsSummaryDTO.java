package br.com.maisprati.ecotecidos.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;

@Schema(description = "Resumo dos descartes de um ponto de coleta")
public record CollectionPointDisposalsSummaryDTO(
        @Schema(description = "Soma total do peso (kg)")
        BigDecimal totalWeightInKg,

        @Schema(description = "Soma total de pontos gerados por este ponto")
        BigDecimal totalEarnedPoints,

        @Schema(description = "Lista paginada dos itens")
        Page<DisposalResponseDTO> disposals
) {}