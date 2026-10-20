package br.com.maisprati.projeto.controller;

import br.com.maisprati.projeto.dto.request.CollectionPointCreateRequestDTO;
import br.com.maisprati.projeto.dto.request.OperatorCreateRequestDTO;
import br.com.maisprati.projeto.dto.response.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import br.com.maisprati.projeto.service.CollectionPointService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/admin/collection-points")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor 
@Tag(name = "Administração de Pontos de Coleta", description = "Endpoints para gerenciamento de pontos de coleta pelo administrador")
public class AdminCollectionPointController {

    private final CollectionPointService collectionPointService;

    @Operation(summary = "Cadastrar Ponto de Coleta", description = "Permite que apenas administradores cadastrem novos pontos de coleta.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ponto de coleta cadastrado com sucesso e aguardando aprovação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CollectionPointResponseDTO.class))),

            @ApiResponse(responseCode = "400", description = "Erro de validação nos campos informados", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ValidationErrorResponseDTO.class))),

            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),

            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil atual", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CollectionPointResponseDTO> createCollectionPoint(
            @RequestPart("data") @Valid CollectionPointCreateRequestDTO requestDTO,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails) {

        CollectionPointResponseDTO response = collectionPointService.createCollectionPoint(requestDTO, image, userDetails.getUsername());

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();

        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar pontos de coleta", description = "Retorna uma lista paginada de pontos de coleta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pontos de coleta listados com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CollectionPointSummaryResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Page<CollectionPointSummaryResponseDTO>> findAll(
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(collectionPointService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ponto de coleta por ID", description = "Retorna os dados detalhados de um ponto de coleta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ponto de coleta encontrado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CollectionPointResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CollectionPointResponseDTO> getCollectionPointById(@PathVariable Long id) {
        return ResponseEntity.ok().body(collectionPointService.getCollectionPointById(id));
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "Obter imagem de um ponto de coleta",
            description = "Retorna a imagem de um ponto de coleta em qualquer status. Disponível somente para administradores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Ponto sem imagem ou arquivo não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Resource> getCollectionPointImage(@PathVariable Long id) throws IOException {
        Resource imageResource = collectionPointService.findImageByCollectionPointIdForAdmin(id);
        String contentType = Files.probeContentType(Paths.get(imageResource.getURI()));
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + imageResource.getFilename() + "\"")
                .body(imageResource);
    }

    @GetMapping("/{id}/pending-image")
    @Operation(summary = "Obter imagem de um ponto de coleta com edição pendente de aprovação",
            description = "Retorna a nova imagem enviada em uma edição pendente. Disponível somente para administradores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem pendente retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "A edição pendente não possui imagem ou o arquivo não foi encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Edição pendente não encontrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Resource> getPendingCollectionPointImage(@PathVariable Long id) throws IOException {
        Resource imageResource = collectionPointService.findPendingImageByCollectionPointIdForAdmin(id);
        String contentType = Files.probeContentType(Paths.get(imageResource.getURI()));
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + imageResource.getFilename() + "\"")
                .body(imageResource);
    }

    @GetMapping("/pending-updates")
    @Operation(summary = "Listar edições de ponto de coleta pendentes de aprovação por um administrador", description = "Retorna os pontos de coleta que aguardam aprovação de edição (hasPendingUpdate = true).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Edições pendentes listadas com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PendingUpdateResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Page<PendingUpdateResponseDTO>> getPendingUpdates(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(collectionPointService.listPendingUpdates(pageable));
    }

    @PostMapping("/{id}/approve-update")
    @Operation(summary = "Aprovar edição de um ponto", description = "Aplica as alterações propostas por um gerente ao ponto de coleta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Edição aprovada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CollectionPointResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta ou edição pendente não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CollectionPointResponseDTO> approvePendingUpdate(@PathVariable Long id) {
        return ResponseEntity.ok(collectionPointService.approvePendingUpdate(id));
    }

    @PostMapping("/{id}/reject-update")
    @Operation(summary = "Rejeitar edição de um ponto", description = "Descarta as alterações propostas e remove o status pendente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Edição rejeitada com sucesso."),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta ou edição pendente não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> rejectPendingUpdate(@PathVariable Long id) {
        collectionPointService.rejectPendingUpdate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar pontos pendentes de aprovação pelo administrador", description = "Retorna uma lista paginada de pontos com status PENDING.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista recuperada com sucesso."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/pending")
    public ResponseEntity<Page<CollectionPointResponseDTO>> getPendingPoints(
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(collectionPointService.findPendingCollectionPoints(pageable));
    }

    @Operation(summary = "Aprovar ponto de coleta", description = "Altera o status do ponto para ACTIVE, tornando-o disponível nos endpoints públicos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ponto de coleta aprovado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CollectionPointResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/{id}/approve")
    public ResponseEntity<CollectionPointResponseDTO> approveCollectionPoint(@PathVariable Long id) {
        CollectionPointResponseDTO response = collectionPointService.approveCollectionPoint(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Rejeitar ponto de coleta", description = "Altera o status do ponto para SUSPENDED, mantendo-o indisponível nos endpoints públicos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ponto de coleta rejeitado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CollectionPointResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/{id}/reject")
    public ResponseEntity<CollectionPointResponseDTO> rejectCollectionPoint(@PathVariable Long id) {
        CollectionPointResponseDTO response = collectionPointService.rejectCollectionPoint(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/pause")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Pausar recebimento do ponto de coleta", description = "Permite que um gerente/proprietário do ponto altere o status para PAUSED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recebimento pausado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CollectionPointResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o usuário atual.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CollectionPointResponseDTO> pauseCollectionPoint(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {

        CollectionPointResponseDTO response = collectionPointService.pauseCollectionPoint(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/users")
    @PreAuthorize("hasAnyRole('PONTO_COLETA_GERENTE', 'ADMIN')")
    @Operation(summary = "Listar operadores e gerentes de um ponto de coleta", description = "Permite listar todos os operadores e gerentes associados a um ponto de coleta específico, desde que o usuário autenticado seja um gerente do ponto ou um administrador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuários do ponto de coleta listados com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CollectionPointUsersResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil de gerente do ponto ou ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CollectionPointUsersResponseDTO> getOperatorsByCollectionPointId(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(collectionPointService.getOperatorsByCollectionPointId(id, userDetails.getUsername()));
    }


    @PatchMapping ("/{id}/users/operators")
    @PreAuthorize("hasAnyRole('PONTO_COLETA_GERENTE', 'ADMIN')")
    @Operation(summary = "Adicionar operador a um ponto de coleta", description = "Permite adicionar um operador a um ponto de coleta específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operador adicionado com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CollectionPointUsersResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Erro de validação nos dados do operador.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ValidationErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil de gerente do ponto ou ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta ou operador não encontrado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CollectionPointUsersResponseDTO> addOperatorToCollectionPoint(
            @PathVariable Long id,
            @RequestBody OperatorCreateRequestDTO operator,
            @AuthenticationPrincipal UserDetails userDetails) {
        CollectionPointUsersResponseDTO response = collectionPointService.addOperatorToCollectionPoint(id, operator, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }



}
