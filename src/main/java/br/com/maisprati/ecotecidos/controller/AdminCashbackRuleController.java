package br.com.maisprati.ecotecidos.controller;

import br.com.maisprati.ecotecidos.dto.response.ErrorResponseDTO;
import br.com.maisprati.ecotecidos.model.entity.CashbackRule;
import br.com.maisprati.ecotecidos.repository.CashbackRuleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/admin/cashback-rules")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administração de Cashback", description = "Configuração e controle de regras globais de pontuação de cashback")
public class AdminCashbackRuleController {

    private final CashbackRuleRepository cashbackRuleRepository;

    @PutMapping("/points-per-kg")
    @Operation(summary = "Atualizar taxa de pontos", description = "Define quantos pontos são gerados por KG de tecido descartado. Requer perfil ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Taxa de pontos atualizada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CashbackRule.class))),
            @ApiResponse(responseCode = "400", description = "Taxa informada menor ou igual a zero, ou formato inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<CashbackRule> updatePointsPerKg(
            @Parameter(description = "Nova quantidade de pontos gerados por KG descartado (deve ser maior que zero)", example = "10.00", required = true)
            @RequestParam BigDecimal newPointsPerKg) {

        if (newPointsPerKg == null || newPointsPerKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("A taxa de pontos por kg deve ser maior que zero.");
        }

        CashbackRule rule = cashbackRuleRepository.findById(1L).orElse(new CashbackRule());
        rule.setPointsPerKg(newPointsPerKg);

        return ResponseEntity.ok(cashbackRuleRepository.save(rule));
    }

    @GetMapping("/current")
    @Operation(summary = "Consultar regra atual", description = "Retorna a configuração atual de conversão de pontos por KG de tecido. Requer perfil ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Regra de cashback atual retornada com sucesso.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CashbackRule.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado. Requer perfil ADMIN.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Regra de cashback padrão ainda não configurada no sistema.",
                    content = @Content)
    })
    public ResponseEntity<CashbackRule> getCurrentRule() {
        return cashbackRuleRepository.findById(1L)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}