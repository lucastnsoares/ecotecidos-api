package br.com.maisprati.ecotecidos.controller;

import br.com.maisprati.ecotecidos.dto.request.UserRegisterRequestDTO;
import br.com.maisprati.ecotecidos.dto.response.ErrorResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.UserSummaryResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.ValidationErrorResponseDTO;
import br.com.maisprati.ecotecidos.service.RegisterUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/register")
@RequiredArgsConstructor
@Tag(name = "Cadastro Público", description = "Endpoints de auto-cadastro de novos doadores")
public class RegisterController {
    private final RegisterUserService registerUserService;

    @Operation(summary = "Registrar doador", description = "Cria uma nova conta de usuário público com o perfil DOADOR.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Doador cadastrado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserSummaryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Inconsistência nos campos enviados ou duplicidade de e-mail/documento",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(anyOf = {ValidationErrorResponseDTO.class, ErrorResponseDTO.class}),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Erro de validação", value = """
                                    {
                                      "timestamp": "2026-08-27T00:00:00Z",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "Erro de validação nos campos enviados.",
                                      "path": "/api/v1/register",
                                      "fieldErrors": [
                                        { "field": "email", "message": "Formato de e-mail inválido." },
                                        { "field": "document", "message": "Documento inválido. Informe um CPF ou CNPJ válido." }
                                      ]
                                    }
                                    """),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Documento já cadastrado", value = """
                                    {
                                      "timestamp": "2026-08-27T00:00:00Z",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "Documento já cadastrado.",
                                      "path": "/api/v1/register"
                                    }
                                    """),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(name = "E-mail já utilizado", value = """
                                    {
                                      "timestamp": "2026-08-27T00:00:00Z",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "E-mail já utilizado.",
                                      "path": "/api/v1/register"
                                    }
                                    """)
                            }))
    })
    @PostMapping
    public ResponseEntity<UserSummaryResponseDTO> registerUser(@RequestBody @Valid UserRegisterRequestDTO dto) {
        var userCreatedDTO = registerUserService.registerUserPublic(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(userCreatedDTO);
    }

}
