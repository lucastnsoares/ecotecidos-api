package br.com.maisprati.ecotecidos.controller;

import br.com.maisprati.ecotecidos.dto.response.PostResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.PostSummaryResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.ErrorResponseDTO;
import br.com.maisprati.ecotecidos.service.PostService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Conteúdo do site", description = "Visualização de posts")
public class PostController {
    private final PostService postService;

    @Operation(summary = "Listar todos os posts publicados de forma resumida e paginada", description = "Retorna os posts publicados de forma resumida. Usuários autenticados podem acessar este endpoint.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista paginada de posts publicados retornada com sucesso."),
        @ApiResponse(responseCode = "400", description = "Parâmetros de consulta inválidos.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class),
                examples = {
                    @ExampleObject(name = "Parâmetro inválido", value = "{\"timestamp\": \"2026-08-27T00:00:00Z\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Requisição com corpo malformado ou parâmetros inválidos.\", \"path\": \"/api/v1/posts\"}"),
                    @ExampleObject(name = "Campo de ordenação inexistente", value = "{\"timestamp\": \"2026-08-27T00:00:00Z\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"O parâmetro 'campoInvalido' não existe para ordenação ou consulta\", \"path\": \"/api/v1/posts\"}")
                })),
        @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class),
                examples = @ExampleObject(value = "{\"timestamp\": \"2026-08-27T00:00:00Z\", \"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Token de autenticação ausente ou inválido.\", \"path\": \"/api/v1/posts\"}")))
    })
    @GetMapping
    public ResponseEntity<Page<PostSummaryResponseDTO>> findAllByPublishedTrue(
        @Parameter(description = "ID da categoria para filtrar os posts. Se não informado, retorna posts de todas as categorias.")
        @RequestParam(required = false) 
        Long categoryId,

        @Parameter(description = "Termo de busca para filtrar os posts.")
        @RequestParam(required = false)
        String searchTerm,

        @Parameter(description = "Lista de tags para filtrar os posts. Se não informado, retorna posts de todas as tags.")
        @RequestParam(required = false)
        Set<String> tags,

        @ParameterObject
        @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.ASC)
        Pageable pageable) {
        return ResponseEntity.ok(postService.findAllByPublishedTrue(categoryId, searchTerm, tags, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retornar um post específico", description = "Retorna os detalhes de um post publicado específico. Usuários autenticados podem acessar este endpoint.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Post publicado localizado com sucesso.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PostResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class),
                examples = @ExampleObject(value = "{\"timestamp\": \"2026-08-27T00:00:00Z\", \"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Token de autenticação ausente ou inválido.\", \"path\": \"/api/v1/posts/1\"}"))),
        @ApiResponse(responseCode = "404", description = "Post inexistente ou não publicado.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class),
                examples = @ExampleObject(value = "{\"timestamp\": \"2026-08-27T00:00:00Z\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Post inexistente ou não disponível.\", \"path\": \"/api/v1/posts/99\"}")))
    })
    public ResponseEntity<PostResponseDTO> findPostByIdAndIsPublishedTrue(
        @Parameter(description = "ID do post publicado", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(postService.findPostByIdAndIsPublishedTrue(id));
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "Obter imagem do post publicado", description = "Retorna o arquivo de imagem do post publicado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Imagem retornada com sucesso.",
            content = @Content(mediaType = "image/*")),
        @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class),
                examples = @ExampleObject(value = "{\"timestamp\": \"2026-08-27T00:00:00Z\", \"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Token de autenticação ausente ou inválido.\", \"path\": \"/api/v1/posts/1/image\"}"))),
        @ApiResponse(responseCode = "404", description = "Post inexistente, não publicado, sem imagem de capa ou arquivo de imagem não encontrado.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class),
                examples = {
                    @ExampleObject(name = "Post inexistente ou não publicado", value = "{\"timestamp\": \"2026-08-27T00:00:00Z\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Post inexistente ou não publicado.\", \"path\": \"/api/v1/posts/99/image\"}"),
                    @ExampleObject(name = "Post sem imagem de capa", value = "{\"timestamp\": \"2026-08-27T00:00:00Z\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Post não possui imagem de capa.\", \"path\": \"/api/v1/posts/1/image\"}"),
                    @ExampleObject(name = "Imagem não encontrada no servidor", value = "{\"timestamp\": \"2026-08-27T00:00:00Z\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Imagem não encontrada no servidor.\", \"path\": \"/api/v1/posts/1/image\"}")
                }))
    })
    public ResponseEntity<Resource> getPublishedPostImage(@PathVariable Long id) throws IOException {
        Resource imageResource = postService.findImageByPostIdAndIsPublishedTrue(id);
        
        String contentType = Files.probeContentType(Paths.get(imageResource.getURI()));

        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                // Define o cabeçalho Content-Type correto
                .contentType(MediaType.parseMediaType(contentType))
                // inline: instrui o cliente a renderizar na tela (em vez de forçar download attachment)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + imageResource.getFilename() + "\"")
                // Cache HTTP: evita requisitar a imagem repetidamente no mesmo cliente
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                // Transmite os bytes via streaming direto do disco
                .body(imageResource);
    }

}
