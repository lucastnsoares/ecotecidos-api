package br.com.maisprati.ecotecidos.controller;

import br.com.maisprati.ecotecidos.dto.request.DisposalCreateRequestDTO;
import br.com.maisprati.ecotecidos.dto.request.DisposalRejectRequestDTO;
import br.com.maisprati.ecotecidos.dto.response.CollectionPointDisposalsSummaryDTO;
import br.com.maisprati.ecotecidos.dto.response.DisposalResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.ErrorResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.UserDisposalsSummaryDTO;
import br.com.maisprati.ecotecidos.dto.response.ValidationErrorResponseDTO;
import br.com.maisprati.ecotecidos.service.DisposalService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/disposals")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Descarte de tecidos", description = "Endpoints para gerenciamento, acompanhamento e validação de descartes de tecidos")
public class DisposalController {

    private final DisposalService disposalService;

    // doadores

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar descarte", description = "Inicia o processo de doação criando um descarte com status PENDING no ponto de coleta informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Descarte registrado com sucesso e aguardando confirmação física.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DisposalResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação, ponto de coleta inativo ou tipos de tecidos não aceitos pelo ponto.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(anyOf = {ValidationErrorResponseDTO.class, ErrorResponseDTO.class}))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta ou usuário não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<DisposalResponseDTO> registerDisposal(
            @Valid @RequestBody DisposalCreateRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disposalService.registerDisposal(request, userDetails.getUsername()));
    }

    @GetMapping("/my-disposals")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Meus descartes", description = "Lista o histórico paginado de descartes e o resumo acumulado (peso total, descartes totais e pontos) do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico e resumo de descartes retornados com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDisposalsSummaryDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<UserDisposalsSummaryDTO> getMyDisposals(
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(disposalService.getUserDisposals(userDetails.getUsername(), pageable));
    }


    //  gerentes / operadores

    @GetMapping("/collection-point/{collectionPointId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PONTO_COLETA_GERENTE', 'PONTO_COLETA_OPERADOR')")
    @Operation(summary = "Histórico do ponto de coleta", description = "Lista todos os descartes realizados no ponto de coleta e o somatório total de peso descartado. Requer perfil de operador/gerente do ponto ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico do ponto de coleta recuperado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CollectionPointDisposalsSummaryDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Usuário não é operador/gerente deste ponto ou ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CollectionPointDisposalsSummaryDTO> getDisposalsByCollectionPoint(
            @PathVariable Long collectionPointId,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(disposalService.getDisposalsByCollectionPoint(collectionPointId, userDetails.getUsername(), pageable));
    }

    @GetMapping("/collection-point/{collectionPointId}/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'PONTO_COLETA_GERENTE', 'PONTO_COLETA_OPERADOR')")
    @Operation(summary = "Descartes pendentes do ponto", description = "Lista de forma paginada apenas os descartes com status PENDING aguardando confirmação no ponto de coleta. Requer perfil de operador/gerente do ponto ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Descartes pendentes retornados com sucesso."),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Usuário não é operador/gerente deste ponto ou ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Page<DisposalResponseDTO>> getPendingDisposalsByCollectionPoint(
            @PathVariable Long collectionPointId,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(disposalService.getPendingDisposals(collectionPointId, userDetails.getUsername(), pageable));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'PONTO_COLETA_GERENTE', 'PONTO_COLETA_OPERADOR')")
    @Operation(summary = "Confirmar descarte e creditar cashback", description = "Confirma o recebimento físico do descarte e credita cashback na carteira do doador (até o limite de 3 descartes com cashback por dia para o mesmo doador neste ponto; além do limite, o descarte é confirmado sem concessão de pontos adicionais).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Descarte confirmado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DisposalResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Descarte não está pendente ou dados inconsistentes.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Usuário não possui permissão de operador/gerente neste ponto.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Descarte não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<DisposalResponseDTO> confirmDisposal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(disposalService.confirmDisposal(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'PONTO_COLETA_GERENTE', 'PONTO_COLETA_OPERADOR')")
    @Operation(summary = "Rejeitar descarte", description = "Rejeita um descarte pendente informando obrigatoriamente uma justificativa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Descarte rejeitado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DisposalResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação na justificativa ou descarte não está pendente.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(anyOf = {ValidationErrorResponseDTO.class, ErrorResponseDTO.class}))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Usuário não possui permissão de operador/gerente neste ponto.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Descarte não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<DisposalResponseDTO> rejectDisposal(
            @PathVariable Long id,
            @Valid @RequestBody DisposalRejectRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(disposalService.rejectDisposal(id, userDetails.getUsername(), request.justification()));
    }
}