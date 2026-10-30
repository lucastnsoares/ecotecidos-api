package br.com.maisprati.projeto.repository;

import br.com.maisprati.projeto.model.entity.CashbackRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashbackRuleRepository extends JpaRepository<CashbackRule, Long> {
}