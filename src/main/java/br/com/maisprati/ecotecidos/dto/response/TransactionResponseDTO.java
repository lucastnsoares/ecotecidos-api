package br.com.maisprati.ecotecidos.dto.response;

import br.com.maisprati.ecotecidos.model.enums.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponseDTO(
        Long id,
        String type, // "CREDIT" ou "DEBIT"
        String description,
        BigDecimal value,
        Instant dateTimeTransaction,
        TransactionStatus status
) {}