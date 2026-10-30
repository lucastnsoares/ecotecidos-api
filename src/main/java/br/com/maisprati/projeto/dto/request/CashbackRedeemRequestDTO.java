package br.com.maisprati.projeto.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CashbackRedeemRequestDTO(
        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor do resgate deve ser maior que zero.")
        BigDecimal amount,

        String description,
        String partnerStore
) {}