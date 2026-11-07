package br.com.maisprati.ecotecidos.service;

import br.com.maisprati.ecotecidos.dto.request.CashbackRedeemRequestDTO;
import br.com.maisprati.ecotecidos.dto.response.CashbackBalanceResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.TransactionResponseDTO;
import br.com.maisprati.ecotecidos.model.entity.CashbackCredit;
import br.com.maisprati.ecotecidos.model.entity.CashbackDebit;
import br.com.maisprati.ecotecidos.model.entity.CashbackWallet;
import br.com.maisprati.ecotecidos.model.entity.Transaction;
import br.com.maisprati.ecotecidos.model.entity.User;
import br.com.maisprati.ecotecidos.model.enums.TransactionStatus;
import br.com.maisprati.ecotecidos.model.enums.TransactionType;
import br.com.maisprati.ecotecidos.repository.CashbackWalletRepository;
import br.com.maisprati.ecotecidos.repository.TransactionRepository;
import br.com.maisprati.ecotecidos.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CashbackService {

    private final CashbackWalletRepository cashbackWalletRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public CashbackBalanceResponseDTO getUserBalance(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        return cashbackWalletRepository.findByUserId(user.getId())
                .map(wallet -> CashbackBalanceResponseDTO.builder()
                        .balance(wallet.getBalance())
                        .updatedAt(wallet.getUpdatedAt())
                        .build())
                .orElseGet(() -> CashbackBalanceResponseDTO.builder()
                        .balance(BigDecimal.ZERO)
                        .updatedAt(Instant.now())
                        .build());
    }

    @Transactional
    public CashbackBalanceResponseDTO redeemCashback(String username, CashbackRedeemRequestDTO request) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        CashbackWallet wallet = cashbackWalletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Carteira não encontrada. Você ainda não possui saldo."));

        // 1. Valida se o usuário tem saldo suficiente
        if (wallet.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para realizar este resgate.");
        }

        // 2. Debita o saldo na carteira
        wallet.setBalance(wallet.getBalance().subtract(request.amount()));
        cashbackWalletRepository.save(wallet);

        // 3. Usa a sua entidade CashbackDebit para registrar o saque
        CashbackDebit debit = new CashbackDebit();
        debit.setWalletId(wallet.getId());
        debit.setDescription(request.description() != null ? request.description() : "Resgate de pontos de cashback");
        debit.setValue(request.amount());
        debit.setDateTimeTransaction(Instant.now());
        debit.setApprovedAt(Instant.now());
        debit.setStatus(TransactionStatus.CONFIRMED);
        debit.setPartnerStore(request.partnerStore());

        transactionRepository.save(debit);

        return CashbackBalanceResponseDTO.builder()
                .balance(wallet.getBalance())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> getTransactionHistory(String username, TransactionType typeFilter, Pageable pageable) {        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        return cashbackWalletRepository.findByUserId(user.getId())
                .map(wallet -> {
                    Page<Transaction> transactions;

                    // Aplica o filtro de Herança para buscar os ganhos ou saques
                    if (typeFilter == TransactionType.CREDIT) {
                        transactions = transactionRepository.findByWalletIdAndType(wallet.getId(), CashbackCredit.class, pageable);
                    } else if (typeFilter == TransactionType.DEBIT) {
                        transactions = transactionRepository.findByWalletIdAndType(wallet.getId(), CashbackDebit.class, pageable);
                    } else {
                        transactions = transactionRepository.findByWalletIdOrderByDateTimeTransactionDesc(wallet.getId(), pageable);
                    }

                    return transactions.map(this::mapToTransactionDTO);
                })
                .orElseGet(Page::empty);
    }

    // Identifica dinamicamente a subclasse para retornar a String amigável pro Front
    private TransactionResponseDTO mapToTransactionDTO(Transaction t) {
        String typeName = t instanceof CashbackCredit ? "CREDIT" : "DEBIT";
        return new TransactionResponseDTO(
                t.getId(),
                typeName,
                t.getDescription(),
                t.getValue(),
                t.getDateTimeTransaction(),
                t.getStatus()
        );
    }
}
