package br.com.maisprati.ecotecidos.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;

@Schema(description = "Resumo dos descartes do usuário")
public record UserDisposalsSummaryDTO(
        @Schema(description = "Soma total do peso (kg) doado pelo usuário")
        BigDecimal totalWeightInKg,
        @Schema(description = "Soma total de pontos ganhos pelo usuário em toda a vida")
        BigDecimal totalEarnedPoints,
        @Schema(description = "Histórico de descartes")
        Page<DisposalResponseDTO> disposals
) {}