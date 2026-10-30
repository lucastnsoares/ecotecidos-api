package br.com.maisprati.ecotecidos.repository;

import br.com.maisprati.ecotecidos.model.entity.CashbackRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashbackRuleRepository extends JpaRepository<CashbackRule, Long> {
}