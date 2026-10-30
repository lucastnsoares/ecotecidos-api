package br.com.maisprati.ecotecidos.controller;

import br.com.maisprati.ecotecidos.dto.request.UserChangePasswordRequestDTO;
import br.com.maisprati.ecotecidos.dto.request.UserEditProfileRequestDTO;
import br.com.maisprati.ecotecidos.dto.response.*;
import br.com.maisprati.ecotecidos.model.entity.User;
import br.com.maisprati.ecotecidos.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "Usuário Logado", description = "Endpoints para gerenciamento do perfil e credenciais do próprio usuário")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class UserController {
    private final UserService userService;

    @Operation(summary = "Consultar dados da conta", description = "Recupera todas as informações do perfil autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados recuperados com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado ou token inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Token de autenticação ausente ou inválido.",
                      "path": "/api/v1/user"
                    }
                    """)))
    })
    @GetMapping
    public ResponseEntity<UserResponseDTO> userData(
            @AuthenticationPrincipal User loggedInUser) {
        var data = userService.getUserData(loggedInUser);
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "Alterar senha da conta", description = "Modifica a senha do usuário após verificação da senha atual.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Senha atual incorreta ou nova senha fora do padrão", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 400,
                      "error": "Bad Request",
                      "message": "A senha atual informada está incorreta.",
                      "path": "/api/v1/user/password"
                    }
                    """))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Token de autenticação ausente ou inválido.",
                      "path": "/api/v1/user/password"
                    }
                    """)))
    })
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @RequestBody @Valid UserChangePasswordRequestDTO dto,
            @AuthenticationPrincipal User loggedInUser) {
        userService.changePassword(loggedInUser, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Editar perfil", description = "Atualiza nome, e-mail e telefone do usuário logado e retorna o novo token JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserUpdatedResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos ou e-mail já cadastrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(anyOf = {
                    ValidationErrorResponseDTO.class, ErrorResponseDTO.class}), examples = {
                    @ExampleObject(name = "Erro de Validação", value = """
                            {
                              "timestamp": "2026-08-27T00:00:00Z",
                              "status": 400,
                              "error": "Bad Request",
                              "message": "Erro de validação nos campos enviados.",
                              "path": "/api/v1/user",
                              "fieldErrors": [
                                { "field": "phone", "message": "Número de telefone inválido no padrão internacional." }
                              ]
                            }
                            """),
                    @ExampleObject(name = "E-mail Duplicado", value = """
                            {
                              "timestamp": "2026-08-27T00:00:00Z",
                              "status": 400,
                              "error": "Bad Request",
                              "message": "E-mail já cadastrado.",
                              "path": "/api/v1/user"
                            }
                            """)
            })),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Token de autenticação ausente ou inválido.",
                      "path": "/api/v1/user"
                    }
                    """)))
    })
    @PatchMapping
    public ResponseEntity<UserUpdatedResponseDTO> editUserData(
            @RequestBody @Valid UserEditProfileRequestDTO dto,
            @AuthenticationPrincipal User loggedInUser) {
        var userUpdated = userService.editUserData(loggedInUser, dto);
        return ResponseEntity.ok(userUpdated);
    }

    @GetMapping("/associations")
    @Operation(summary = "Listar associações do usuário", description = "Retorna lista de pontos de coleta associados ao usuário e seus respectivos papéis.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Associações recuperadas com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserAssociationsResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Token de autenticação ausente ou inválido.",
                      "path": "/api/v1/user/user-associations"
                    }
                    """)))
    })
    public ResponseEntity<UserAssociationsResponseDTO> getUserAssociations(
            @AuthenticationPrincipal User loggedInUser) {
        var associations = userService.getUserAssociations(loggedInUser);
        return ResponseEntity.ok(associations);
    }

    @GetMapping("/favorite-posts")
    @Operation(summary = "Listar posts favoritos", description = "Retorna, de forma paginada, os posts favoritos do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posts favoritos recuperados com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PostSummaryResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Token de autenticação ausente ou inválido.",
                      "path": "/api/v1/user/favorite-posts"
                    }
                    """)))
    })
    public ResponseEntity<Page<PostSummaryResponseDTO>> getFavoritePosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostSummaryResponseDTO> response = userService.getFavoritePosts(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/favorite-posts/count")
    @Operation(summary = "Consultar quantidade de posts favoritos", description = "Retorna a quantidade de posts favoritos do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantidade recuperada com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Long.class, example = "5"))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Token de autenticação ausente ou inválido.",
                      "path": "/api/v1/user/favorite-posts/count"
                    }
                    """)))
    })
    public ResponseEntity<Long> getFavoritePostsCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long count = userService.getFavoritePostsCount(userDetails.getUsername());
        return ResponseEntity.ok(count);
    }

    @GetMapping("/favorite-posts/{postId}")
    @Operation(summary = "Consulta se um post foi favoritado", description = "Retorna se determinado post foi favoritado pelo usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta realizada com sucesso (true se favoritado, false caso contrário)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Boolean.class, example = "true"))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Token de autenticação ausente ou inválido.",
                      "path": "/api/v1/user/favorite-posts/1"
                    }
                    """)))
    })
    public ResponseEntity<Boolean> checkFavoritePost(
            @AuthenticationPrincipal UserDetails userDetails,
            @ParameterObject @PathVariable Long postId) {
        Boolean response = userService.checkFavoritePost(userDetails.getUsername(), postId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/favorite-posts/{postId}")
    @Operation(summary = "Adicionar post favorito", description = "Adiciona um post aos favoritos do usuário autenticado. A operação é idempotente: favoritar um post já favoritado não gera erro.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Post adicionado aos favoritos com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Token de autenticação ausente ou inválido.",
                      "path": "/api/v1/user/favorite-posts/1"
                    }
                    """))),
            @ApiResponse(responseCode = "404", description = "Post ou usuário não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 404,
                      "error": "Not Found",
                      "message": "Post não encontrado.",
                      "path": "/api/v1/user/favorite-posts/1"
                    }
                    """)))
    })
    public ResponseEntity<Void> favoritePost(
            @AuthenticationPrincipal UserDetails userDetails,
            @ParameterObject @PathVariable Long postId) {
        userService.favoritePost(userDetails.getUsername(), postId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/favorite-posts/{postId}")
    @Operation(summary = "Remover post favorito", description = "Remove um post dos favoritos do usuário autenticado. A operação é idempotente: remover um post que não está favoritado não gera erro.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Post removido dos favoritos com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Token de autenticação ausente ou inválido.",
                      "path": "/api/v1/user/favorite-posts/1"
                    }
                    """))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 404,
                      "error": "Not Found",
                      "message": "Usuário não encontrado.",
                      "path": "/api/v1/user/favorite-posts/1"
                    }
                    """)))
    })
    public ResponseEntity<Void> unfavoritePost(
            @AuthenticationPrincipal UserDetails userDetails,
            @ParameterObject
            @PathVariable Long postId) {
        userService.unfavoritePost(userDetails.getUsername(), postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/favorite-collection-points")
    @Operation(summary = "Listar pontos de coleta favoritos", description = "Retorna, de forma paginada, os pontos de coleta favoritos do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pontos de coleta favoritos recuperados com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CollectionPointSummaryResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Token de autenticação ausente ou inválido.",
                      "path": "/api/v1/user/favorite-collection-points"
                    }
                    """)))
    })
    public ResponseEntity<Page<CollectionPointSummaryResponseDTO>> getFavoriteCollectionPoints(
            @AuthenticationPrincipal UserDetails userDetails,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(userService.getFavoriteCollectionPoints(userDetails.getUsername(), pageable));
    }

    @GetMapping("/favorite-collection-points/count")
    @Operation(summary = "Consultar quantidade de pontos de coleta favoritos", description = "Retorna a quantidade de pontos de coleta favoritos do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantidade recuperada com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Long.class, example = "3"))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Token de autenticação ausente ou inválido.",
                      "path": "/api/v1/user/favorite-collection-points/count"
                    }
                    """)))
    })
    public ResponseEntity<Long> getFavoriteCollectionPointsCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getFavoriteCollectionPointsCount(userDetails.getUsername()));
    }

    @PutMapping("/favorite-collection-points/{collectionPointId}")
    @Operation(summary = "Adicionar ponto de coleta favorito", description = "Adiciona um ponto de coleta aos favoritos do usuário autenticado. A operação é idempotente: favoritar um ponto de coleta já favoritado não gera erro.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ponto de coleta adicionado aos favoritos com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Token de autenticação ausente ou inválido.",
                      "path": "/api/v1/user/favorite-collection-points/1"
                    }
                    """))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta ou usuário não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 404,
                      "error": "Not Found",
                      "message": "Ponto de coleta não encontrado.",
                      "path": "/api/v1/user/favorite-collection-points/1"
                    }
                    """)))
    })
    public ResponseEntity<Void> favoriteCollectionPoint(
            @AuthenticationPrincipal UserDetails userDetails,
            @ParameterObject
            @PathVariable Long collectionPointId) {
        userService.favoriteCollectionPoint(userDetails.getUsername(), collectionPointId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/favorite-collection-points/{collectionPointId}")
    @Operation(summary = "Remover ponto de coleta favorito", description = "Remove um ponto de coleta dos favoritos do usuário autenticado. A operação é idempotente: remover um ponto de coleta que não está favoritado não gera erro.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ponto de coleta removido dos favoritos com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Token de autenticação ausente ou inválido.",
                      "path": "/api/v1/user/favorite-collection-points/1"
                    }
                    """))),
            @ApiResponse(responseCode = "404", description = "Ponto de coleta ou usuário não encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class), examples = @ExampleObject(value = """
                    {
                      "timestamp": "2026-08-27T00:00:00Z",
                      "status": 404,
                      "error": "Not Found",
                      "message": "Ponto de coleta não encontrado.",
                      "path": "/api/v1/user/favorite-collection-points/1"
                    }
                    """)))
    })
    public ResponseEntity<Void> unfavoriteCollectionPoint(
            @AuthenticationPrincipal UserDetails userDetails,
            @ParameterObject
            @PathVariable Long collectionPointId) {
        userService.unfavoriteCollectionPoint(userDetails.getUsername(), collectionPointId);
        return ResponseEntity.noContent().build();
    }
}
