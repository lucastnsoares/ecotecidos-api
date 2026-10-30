package br.com.maisprati.ecotecidos.repository;

import br.com.maisprati.ecotecidos.model.entity.CashbackWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CashbackWalletRepository extends JpaRepository<CashbackWallet, Long> {
    Optional<CashbackWallet> findByUserId(Long userId);
}