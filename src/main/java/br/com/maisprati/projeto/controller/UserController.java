package br.com.maisprati.projeto.controller;

import br.com.maisprati.projeto.dto.request.UserChangePasswordRequestDTO;
import br.com.maisprati.projeto.dto.request.UserEditProfileRequestDTO;
import br.com.maisprati.projeto.dto.response.*;
import br.com.maisprati.projeto.model.entity.User;
import br.com.maisprati.projeto.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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


        @Operation(summary = "Editar perfil", description = "Atualiza os dados cadastrais e, opcionalmente, substitui ou remove o avatar. A parte data deve conter o JSON do perfil.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso", content = @Content(schema = @Schema(implementation = UserUpdatedResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Dados ou arquivo de imagem inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PatchMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<UserUpdatedResponseDTO> editUserDataWithImage(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                        content = @Content(
                                                        encoding = @Encoding(name = "data", contentType = "application/json")))
                        @Valid @RequestPart("data") UserEditProfileRequestDTO dto,
                        @Parameter(description = "Novo avatar do usuário (JPEG, PNG ou WebP)", example = "avatar.jpg")
                        @RequestPart(value = "image", required = false) MultipartFile image,
                        @AuthenticationPrincipal User loggedInUser) {
                return ResponseEntity.ok(userService.editUserData(loggedInUser, dto, image));
        }

        @GetMapping("/avatar")
        @Operation(summary = "Obter próprio avatar", description = "Retorna somente a imagem de perfil do usuário autenticado.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Imagem retornada com sucesso"),
                        @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido",
                                        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Usuário não encontrado ou sem imagem",
                                        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        public ResponseEntity<Resource> getAvatar(@AuthenticationPrincipal User loggedInUser) throws IOException {
                return buildImageResponse(userService.getAvatar(loggedInUser));
        }

        private ResponseEntity<Resource> buildImageResponse(Resource avatar) throws IOException {
                String contentType = Files.probeContentType(Paths.get(avatar.getURI()));
                if (contentType == null) {
                        contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }

                return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(contentType))
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "inline; filename=\"" + avatar.getFilename() + "\"")
                                .cacheControl(CacheControl.noStore())
                                .body(avatar);
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

        @GetMapping("/balance")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Consultar saldo", description = "Retorna o saldo atual de pontos de cashback do usuário autenticado.")
        public ResponseEntity<CashbackBalanceResponseDTO> getBalance(@AuthenticationPrincipal UserDetails userDetails) {

                CashbackBalanceResponseDTO response = userService.getUserBalance(userDetails.getUsername());
                return ResponseEntity.ok(response);
        }
}
