package br.com.maisprati.projeto.controller;

import br.com.maisprati.projeto.dto.request.DisposalCreateRequestDTO;
import br.com.maisprati.projeto.dto.request.DisposalRejectRequestDTO;
import br.com.maisprati.projeto.dto.response.CollectionPointDisposalsSummaryDTO;
import br.com.maisprati.projeto.dto.response.DisposalResponseDTO;
import br.com.maisprati.projeto.dto.response.UserDisposalsSummaryDTO;
import br.com.maisprati.projeto.service.DisposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/disposals")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Descarte de tecidos", description = "Endpoints para gerenciamento e aceite de descartes")
public class DisposalController {

    private final DisposalService disposalService;

    // doadores

    @PostMapping
    @PreAuthorize("isAuthenticated()") // Se desejar fechar apenas para usuários, use hasRole('USER')
    @Operation(summary = "Registrar descarte", description = "Inicia o processo de doação gerando um descarte PENDENTE.")
    public ResponseEntity<DisposalResponseDTO> registerDisposal(
            @Valid @RequestBody DisposalCreateRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disposalService.registerDisposal(request, userDetails.getUsername()));
    }

    @GetMapping("/my-disposals")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Meus descartes", description = "Lista o histórico de descartes e o resumo de totais do usuário autenticado.")
    public ResponseEntity<UserDisposalsSummaryDTO> getMyDisposals(
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(disposalService.getUserDisposals(userDetails.getUsername(), pageable));
    }


    //  gerentes / operadores

    @GetMapping("/collection-point/{collectionPointId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PONTO_COLETA_GERENTE', 'PONTO_COLETA_OPERADOR')")
    @Operation(summary = "Histórico do ponto", description = "Lista TODOS os descartes e o peso total (Apenas Gerentes/Operadores).")
    public ResponseEntity<CollectionPointDisposalsSummaryDTO> getDisposalsByCollectionPoint(
            @PathVariable Long collectionPointId,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(disposalService.getDisposalsByCollectionPoint(collectionPointId, userDetails.getUsername(), pageable));
    }

    @GetMapping("/collection-point/{collectionPointId}/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'PONTO_COLETA_GERENTE', 'PONTO_COLETA_OPERADOR')")
    @Operation(summary = "Descartes Pendentes", description = "Lista apenas descartes aguardando aprovação para gerar cashback (SCRUM-374).")
    public ResponseEntity<Page<DisposalResponseDTO>> getPendingDisposalsByCollectionPoint(
            @PathVariable Long collectionPointId,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(disposalService.getPendingDisposals(collectionPointId, userDetails.getUsername(), pageable));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'PONTO_COLETA_GERENTE', 'PONTO_COLETA_OPERADOR')")
    @Operation(summary = "Aceitar e gerar Cashback", description = "Confirma a entrega do descarte, aplica regras do ponto e lança o crédito na carteira do usuário (SCRUM-375).")
    public ResponseEntity<DisposalResponseDTO> confirmDisposal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(disposalService.confirmDisposal(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'PONTO_COLETA_GERENTE', 'PONTO_COLETA_OPERADOR')")
    @Operation(summary = "Rejeitar descarte", description = "Rejeita um descarte informando uma justificativa.")
    public ResponseEntity<DisposalResponseDTO> rejectDisposal(
            @PathVariable Long id,
            @Valid @RequestBody DisposalRejectRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(disposalService.rejectDisposal(id, userDetails.getUsername(), request.justification()));
    }
}