package br.com.maisprati.projeto.repository;

import br.com.maisprati.projeto.model.entity.CashbackCredit;
import br.com.maisprati.projeto.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT c FROM CashbackCredit c WHERE c.status = 'CONFIRMED' AND c.dateTimeExpiration < :now")
    List<CashbackCredit> findExpiredCredits(@Param("now") Instant now);

}