package br.com.maisprati.ecotecidos.controller;

import br.com.maisprati.ecotecidos.dto.request.CashbackRedeemRequestDTO;
import br.com.maisprati.ecotecidos.dto.response.CashbackBalanceResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.ErrorResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.TransactionResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.ValidationErrorResponseDTO;
import br.com.maisprati.ecotecidos.model.enums.TransactionType;
import br.com.maisprati.ecotecidos.service.CashbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cashback")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cashback e Carteira", description = "Endpoints para consulta de saldo, histórico e resgate de pontos de cashback")
public class CashbackController {

    private final CashbackService cashbackService;

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar saldo", description = "Retorna o saldo atual de pontos de cashback acumulados na carteira do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo de cashback retornado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CashbackBalanceResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Carteira de cashback não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CashbackBalanceResponseDTO> getBalance(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cashbackService.getUserBalance(userDetails.getUsername()));
    }

    @PostMapping("/redeem")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Resgatar saldo (Saque)", description = "Debita o saldo de pontos da carteira e registra uma transação de débito.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resgate efetuado com sucesso e saldo atualizado retornado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CashbackBalanceResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação ou saldo insuficiente para resgate.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(anyOf = {ValidationErrorResponseDTO.class, ErrorResponseDTO.class}))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Carteira de cashback não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CashbackBalanceResponseDTO> redeemCashback(
            @Valid @RequestBody CashbackRedeemRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cashbackService.redeemCashback(userDetails.getUsername(), request));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Histórico da carteira", description = "Lista as transações de cashback do usuário autenticado de forma paginada. Permite filtrar opcionalmente por tipo de transação (CREDIT ou DEBIT).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico de transações retornado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Tipo de filtro ou parâmetros de paginação inválidos.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Page<TransactionResponseDTO>> getHistory(
            @Parameter(description = "Filtro pelo tipo de transação", example = "CREDIT") @RequestParam(required = false) TransactionType filter,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cashbackService.getTransactionHistory(userDetails.getUsername(), filter, pageable));
    }
}