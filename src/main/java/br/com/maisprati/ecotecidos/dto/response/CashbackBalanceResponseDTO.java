package br.com.maisprati.ecotecidos.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
@Schema(description = "Resposta contendo o saldo atual de cashback do usuário")
public record CashbackBalanceResponseDTO(

        @Schema(description = "Saldo atual disponível (em pontos/moeda)", example = "150.00")
        BigDecimal balance,

        @Schema(description = "Data e hora da última atualização do saldo")
        Instant updatedAt
) {
}