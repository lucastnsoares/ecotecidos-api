package br.com.maisprati.projeto.service;

import br.com.maisprati.projeto.model.entity.CashbackCredit;
import br.com.maisprati.projeto.model.entity.CashbackWallet;
import br.com.maisprati.projeto.model.enums.TransactionStatus;
import br.com.maisprati.projeto.repository.CashbackWalletRepository;
import br.com.maisprati.projeto.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashbackExpirationService {

    private final TransactionRepository transactionRepository;
    private final CashbackWalletRepository cashbackWalletRepository;

    // Roda todos os dias às 00:01h (Meia-noite e um)
    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void expireOldCashbackCredits() {
        log.info("Iniciando varredura de cashbacks expirados...");

        // Busca todas as transações de crédito confirmadas que já passaram da data de expiração
        List<CashbackCredit> expiredCredits = transactionRepository.findExpiredCredits(Instant.now());

        int count = 0;
        for (CashbackCredit credit : expiredCredits) {
            CashbackWallet wallet = cashbackWalletRepository.findById(credit.getWalletId()).orElse(null);

            if (wallet != null) {
                // Desconta o saldo expirado (garantindo que o saldo da carteira não fique negativo caso ele já tenha gastado parte)
                BigDecimal currentBalance = wallet.getBalance();
                BigDecimal newBalance = currentBalance.subtract(credit.getValue());

                wallet.setBalance(newBalance.compareTo(BigDecimal.ZERO) > 0 ? newBalance : BigDecimal.ZERO);
                cashbackWalletRepository.save(wallet);

                // Marca a transação original como expirada
                credit.setStatus(TransactionStatus.EXPIRED);
                transactionRepository.save(credit);
                count++;
            }
        }

        log.info("Varredura concluída. {} cashbacks expirados e descontados.", count);
    }
}