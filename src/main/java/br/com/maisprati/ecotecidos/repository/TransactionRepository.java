package br.com.maisprati.ecotecidos.repository;

import br.com.maisprati.ecotecidos.model.entity.CashbackCredit;
import br.com.maisprati.ecotecidos.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT c FROM CashbackCredit c WHERE c.status = 'CONFIRMED' AND c.dateTimeExpiration < :now")
    List<CashbackCredit> findExpiredCredits(@Param("now") Instant now);

    // Busca o histórico (Ganhos e Saques)
    Page<Transaction> findByWalletIdOrderByDateTimeTransactionDesc(Long walletId, Pageable pageable);

    // Filtra utilizando as subclasses de wallet
    @Query("SELECT t FROM Transaction t WHERE t.walletId = :walletId AND TYPE(t) = :type ORDER BY t.dateTimeTransaction DESC")
    Page<Transaction> findByWalletIdAndType(@Param("walletId") Long walletId, @Param("type") Class<? extends Transaction> type, Pageable pageable);
}