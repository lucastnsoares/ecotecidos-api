package br.com.maisprati.projeto.repository;

import br.com.maisprati.projeto.model.entity.Disposal;
import br.com.maisprati.projeto.model.enums.DisposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;

@Repository
public interface DisposalRepository extends JpaRepository<Disposal, Long> {

    // Retorna os descartes de um ponto específico paginados
    Page<Disposal> findByCollectionPointId(Long collectionPointId, Pageable pageable);

    // Listar descartes de um usuário específico
    Page<Disposal> findByUserId(Long userId, Pageable pageable);

    // Listar descartes pendentes de um ponto específico
    Page<Disposal> findByCollectionPointIdAndStatus(Long collectionPointId, DisposalStatus status, Pageable pageable);

    // Calcula a soma total de todos os descartes (poderia adicionar AND d.status = 'CONFIRMED' se desejar somar só os aceitos)
    @Query("SELECT COALESCE(SUM(d.approximateWeightInKg), 0) FROM Disposal d WHERE d.collectionPoint.id = :collectionPointId")
    BigDecimal getTotalWeightByCollectionPointId(@Param("collectionPointId") Long collectionPointId);

    @Query("SELECT COUNT(d) FROM Disposal d " +
            "WHERE d.user.id = :userId " +
            "AND d.collectionPoint.id = :pointId " +
            "AND d.status = 'CONFIRMED' " +
            "AND d.confirmedAt >= :startOfDay " +
            "AND d.cashbackCredit IS NOT NULL")
    long countDisposalsWithCashbackToday(
            @Param("userId") Long userId,
            @Param("pointId") Long pointId,
            @Param("startOfDay") Instant startOfDay
    );

    // Totais do Ponto de Coleta
    @Query("SELECT COALESCE(SUM(c.value), 0) FROM Disposal d JOIN d.cashbackCredit c WHERE d.collectionPoint.id = :pointId AND d.status = 'CONFIRMED'")
    BigDecimal getTotalEarnedPointsByCollectionPointId(@Param("pointId") Long pointId);

    // Totais do Usuário
    @Query("SELECT COALESCE(SUM(d.approximateWeightInKg), 0) FROM Disposal d WHERE d.user.id = :userId AND d.status = 'CONFIRMED'")
    BigDecimal getTotalWeightByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(c.value), 0) FROM Disposal d JOIN d.cashbackCredit c WHERE d.user.id = :userId AND d.status = 'CONFIRMED'")
    BigDecimal getTotalEarnedPointsByUserId(@Param("userId") Long userId);
}