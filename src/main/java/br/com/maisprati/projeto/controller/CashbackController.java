package br.com.maisprati.projeto.controller;

import br.com.maisprati.projeto.dto.request.CashbackRedeemRequestDTO;
import br.com.maisprati.projeto.dto.response.CashbackBalanceResponseDTO;
import br.com.maisprati.projeto.dto.response.TransactionResponseDTO;
import br.com.maisprati.projeto.service.CashbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cashback")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cashback e Carteira", description = "Endpoints para consulta de saldo, histórico e resgate")
public class CashbackController {

    private final CashbackService cashbackService;

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar saldo", description = "Retorna o saldo atual de pontos de cashback do usuário.")
    public ResponseEntity<CashbackBalanceResponseDTO> getBalance(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cashbackService.getUserBalance(userDetails.getUsername()));
    }

    @PostMapping("/redeem")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Resgatar saldo (Saque)", description = "Debita o saldo da carteira e registra uma transação de CashbackDebit.")
    public ResponseEntity<CashbackBalanceResponseDTO> redeemCashback(
            @Valid @RequestBody CashbackRedeemRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cashbackService.redeemCashback(userDetails.getUsername(), request));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Histórico da carteira", description = "Lista as transações. Filtre opcionalmente por filter=CREDIT ou filter=DEBIT.")
    public ResponseEntity<Page<TransactionResponseDTO>> getHistory(
            @RequestParam(required = false) String filter,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cashbackService.getTransactionHistory(userDetails.getUsername(), filter, pageable));
    }
}