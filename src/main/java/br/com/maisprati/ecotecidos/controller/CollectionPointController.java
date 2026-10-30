package br.com.maisprati.ecotecidos.controller;

import br.com.maisprati.ecotecidos.dto.request.CollectionPointUpdateRequestDTO;
import br.com.maisprati.ecotecidos.dto.request.CollectionPointCreateRequestDTO;
import br.com.maisprati.ecotecidos.dto.response.CollectionPointResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.CollectionPointDistanceResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.ErrorResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.ValidationErrorResponseDTO;
import br.com.maisprati.ecotecidos.service.CollectionPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/collection-points")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pontos de Coleta", description = "Endpoints para gerenciamento de pontos de coleta")
public class CollectionPointController {

        private final CollectionPointService collectionPointService;

        @GetMapping("/{id}")
        @Operation(summary = "Buscar ponto de coleta por ID", description = "Permite buscar um ponto de coleta ativo pelo seu ID, exibindo-o de forma detalhada.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Ponto de coleta encontrado com sucesso.", content = @Content(schema = @Schema(implementation = CollectionPointResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Ponto de coleta não encontrado ou inativo.", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<CollectionPointResponseDTO> getCollectionPointByIdAndStatusActiveEntity(
                        @PathVariable Long id) {
                CollectionPointResponseDTO response = collectionPointService.getCollectionPointByIdAndStatusActive(id);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Listar pontos ativos no mapa", description = "SCRUM-179: Retorna apenas pontos de coleta com status ACTIVE.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Pontos de coleta ativos listados com sucesso.", content = @Content(schema = @Schema(implementation = CollectionPointResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos.", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @GetMapping
        public ResponseEntity<Page<CollectionPointResponseDTO>> getPublicApprovedPoints(
                        @ParameterObject @PageableDefault(page = 0, size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
                return ResponseEntity.ok(collectionPointService.findPublicApprovedPoints(pageable));
        }

        @GetMapping("/nearby")
        @Operation(summary = "Buscar pontos de coleta mais próximos por raio de distância")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Pontos de coleta próximos encontrados com sucesso.", content = @Content(schema = @Schema(implementation = CollectionPointDistanceResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Parâmetros de localização, raio ou paginação inválidos.", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<Page<CollectionPointDistanceResponseDTO>> findNearby(
                        @Parameter(description = "Latitude do usuário", example = "-19.9167", required = true) @RequestParam BigDecimal lat,
                        @Parameter(description = "Longitude do usuário", example = "-43.9345", required = true) @RequestParam BigDecimal lng,
                        @Parameter(description = "Raio máximo de busca em km (padrão 15km)", example = "10.0") @RequestParam(required = false, defaultValue = "15.0") Double radiusKm,
                        @Parameter(description = "IDs dos tipos de tecidos a serem considerados na busca", example = "[1, 2, 3]") @RequestParam(required = false) List<Long> clothTypeIds,
                        @ParameterObject @PageableDefault(page = 0, size = 10) Pageable pageable) {
                return ResponseEntity.ok(collectionPointService.findNearby(lat, lng, radiusKm, clothTypeIds, pageable));
        }

        @GetMapping("/{id}/image")
        @Operation(summary = "Obter imagem do ponto de coleta", description = "Retorna a imagem de um ponto de coleta ativo.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Imagem retornada com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Ponto inexistente, inativo ou sem imagem", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<Resource> getCollectionPointImage(@PathVariable Long id) throws IOException {
                Resource imageResource = collectionPointService.findImageByCollectionPointId(id);
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

        @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Atualizar Ponto de Coleta", description = "Gerentes e administradores enviam alterações para aprovação administrativa. A alteração não é aplicada imediatamente ao ponto.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Ponto de coleta atualizado ou solicitação criada com sucesso.", content = @Content(schema = @Schema(implementation = CollectionPointResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Erro de validação nos campos informados.", content = @Content(schema = @Schema(implementation = ValidationErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "403", description = "Acesso negado para o usuário atual.", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Ponto de coleta não encontrado.", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<CollectionPointResponseDTO> updateCollectionPoint(
                        @PathVariable Long id,
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(encoding = @Encoding(name = "data", contentType = "application/json"))) @RequestPart("data") @Valid CollectionPointUpdateRequestDTO requestDTO,
                        @RequestPart(value = "image", required = false) MultipartFile image,
                        @AuthenticationPrincipal UserDetails userDetails) {

                CollectionPointResponseDTO response = collectionPointService.updateCollectionPoint(id, requestDTO,
                                image, userDetails.getUsername());
                return ResponseEntity.ok(response);
        }

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Criar Ponto de Coleta", description = "Solicita a criação de um novo ponto de coleta.")
        @ApiResponses({
                        @ApiResponse(responseCode = "202", description = "Solicitação de criação recebida com sucesso.", content = @Content(schema = @Schema(implementation = CollectionPointResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Erro de validação nos campos informados.", content = @Content(schema = @Schema(implementation = ValidationErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "403", description = "Acesso negado para o usuário atual.", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<CollectionPointResponseDTO> createCollectionPoint(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(encoding = @Encoding(name = "data", contentType = "application/json"))) @RequestPart("data") @Valid CollectionPointCreateRequestDTO dto,
                        @RequestPart(value = "image", required = false) MultipartFile image,
                        @AuthenticationPrincipal UserDetails userDetails) {

                CollectionPointResponseDTO response = collectionPointService.createCollectionPoint(dto, image,
                                userDetails.getUsername());
                URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                                .path("/{id}")
                                .buildAndExpand(response.getId())
                                .toUri();

                return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).body(response);
        }

}