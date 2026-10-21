package br.com.maisprati.projeto.controller;

import br.com.maisprati.projeto.model.entity.CashbackRule;
import br.com.maisprati.projeto.repository.CashbackRuleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/admin/cashback-rules")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administração de Cashback", description = "Configuração de regras globais de pontos")
public class AdminCashbackRuleController {

    private final CashbackRuleRepository cashbackRuleRepository;

    @PutMapping("/points-per-kg")
    @Operation(summary = "Atualizar taxa de pontos", description = "Define quantos pontos são gerados por KG de tecido descartado.")
    public ResponseEntity<CashbackRule> updatePointsPerKg(@RequestParam BigDecimal newPointsPerKg) {

        CashbackRule rule = cashbackRuleRepository.findById(1L).orElse(new CashbackRule());
        rule.setPointsPerKg(newPointsPerKg);

        return ResponseEntity.ok(cashbackRuleRepository.save(rule));
    }

    @GetMapping("/current")
    @Operation(summary = "Ver regra atual")
    public ResponseEntity<CashbackRule> getCurrentRule() {
        return cashbackRuleRepository.findById(1L)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}