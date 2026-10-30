package br.com.maisprati.projeto.repository;

import br.com.maisprati.projeto.model.entity.CashbackWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CashbackWalletRepository extends JpaRepository<CashbackWallet, Long> {
    Optional<CashbackWallet> findByUserId(Long userId);
}