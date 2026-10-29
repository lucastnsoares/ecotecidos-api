package br.com.maisprati.projeto.dto.response;

import br.com.maisprati.projeto.model.enums.TransactionStatus;
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