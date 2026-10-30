package br.com.maisprati.projeto.service;

import br.com.maisprati.projeto.dto.request.DisposalCreateRequestDTO;
import br.com.maisprati.projeto.dto.response.CollectionPointDisposalsSummaryDTO;
import br.com.maisprati.projeto.dto.response.DisposalResponseDTO;
import br.com.maisprati.projeto.dto.response.UserDisposalsSummaryDTO;
import br.com.maisprati.projeto.model.entity.*;
import br.com.maisprati.projeto.model.enums.CollectionPointStatus;
import br.com.maisprati.projeto.model.enums.DisposalStatus;
import br.com.maisprati.projeto.model.enums.Role;
import br.com.maisprati.projeto.model.enums.TransactionStatus;
import br.com.maisprati.projeto.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisposalService {

    private final DisposalRepository disposalRepository;
    private final UserRepository userRepository;
    private final CashbackRuleRepository cashbackRuleRepository;
    private final CollectionPointRepository collectionPointRepository;
    private final ClothTypeRepository clothTypeRepository;
    private final CashbackWalletRepository cashbackWalletRepository;
    private final TransactionRepository transactionRepository;

    private static final int CASHBACK_EXPIRATION_DAYS = 30;
    private static final int MAX_CASHBACK_DISPOSALS_PER_DAY = 3;

    @Transactional
    public DisposalResponseDTO registerDisposal(DisposalCreateRequestDTO dto, String username) {
        User currentUser = userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        CollectionPoint point = collectionPointRepository.findById(dto.collectionPointId())
                .orElseThrow(() -> new EntityNotFoundException("Ponto de coleta não encontrado."));

        if (point.getStatus() != CollectionPointStatus.ACTIVE) {
            throw new IllegalStateException("Este ponto de coleta não está recebendo doações no momento.");
        }

        // Validação: Verificar se o ponto aceita os tecidos informados (Ajuste Solicitado)
        Set<Long> acceptedClothTypeIds = point.getClothTypes().stream()
                .map(ClothType::getId)
                .collect(Collectors.toSet());

        if (!acceptedClothTypeIds.containsAll(dto.clothTypeIds())) {
            throw new IllegalArgumentException("O ponto de coleta selecionado não aceita um ou mais tipos de tecidos informados.");
        }

        List<ClothType> clothTypes = clothTypeRepository.findAllById(dto.clothTypeIds());

        Disposal disposal = Disposal.builder()
                .user(currentUser)
                .collectionPoint(point)
                .clothTypes(new HashSet<>(clothTypes))
                .approximateWeightInKg(dto.approximateWeightInKg())
                .status(DisposalStatus.PENDING)
                .build();

        return mapToDTO(disposalRepository.save(disposal));
    }

    // Listar descartes do próprio usuário logado
    @Transactional(readOnly = true)
    public UserDisposalsSummaryDTO getUserDisposals(String username, Pageable pageable) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        Page<DisposalResponseDTO> disposalsPage = disposalRepository.findByUserId(user.getId(), pageable).map(this::mapToDTO);
        BigDecimal totalKg = disposalRepository.getTotalWeightByUserId(user.getId());
        BigDecimal totalPoints = disposalRepository.getTotalEarnedPointsByUserId(user.getId());

        return new UserDisposalsSummaryDTO(totalKg, totalPoints, disposalsPage);
    }

    // Listar descartes pendentes para o operador aprovar (SCRUM-374)
    @Transactional(readOnly = true)
    public Page<DisposalResponseDTO> getPendingDisposals(Long collectionPointId, String username, Pageable pageable) {
        validatePointManagerOrOperator(collectionPointId, username);
        return disposalRepository.findByCollectionPointIdAndStatus(collectionPointId, DisposalStatus.PENDING, pageable)
                .map(this::mapToDTO);
    }

    // Confirmação do descarte, cálculo do cashback e inserção na carteira (SCRUM-375 a 400)
    @Transactional
    public DisposalResponseDTO confirmDisposal(Long disposalId, String approverUsername) {
        Disposal disposal = disposalRepository.findById(disposalId)
                .orElseThrow(() -> new EntityNotFoundException("Descarte não encontrado."));

        if (disposal.getStatus() != DisposalStatus.PENDING) {
            throw new IllegalStateException("Apenas descartes pendentes podem ser confirmados.");
        }

        User approver = validatePointManagerOrOperator(disposal.getCollectionPoint().getId(), approverUsername);
        User donor = disposal.getUser();

        disposal.setStatus(DisposalStatus.CONFIRMED);
        disposal.setConfirmedBy(approver);
        disposal.setConfirmedAt(Instant.now());

        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        long disposalsWithCashbackToday = disposalRepository.countDisposalsWithCashbackToday(
                donor.getId(),
                disposal.getCollectionPoint().getId(),
                startOfDay
        );

        if (disposalsWithCashbackToday < MAX_CASHBACK_DISPOSALS_PER_DAY) {
            // 1. Busca a regra de pontos vigente no banco
            CashbackRule rule = cashbackRuleRepository.findById(1L)
                    .orElse(CashbackRule.builder().pointsPerKg(new BigDecimal("10.00")).build());

            // 2. Calcula o valor total de pontos ganhos
            BigDecimal cashbackValue = disposal.getApproximateWeightInKg().multiply(rule.getPointsPerKg());

            // 3. Atualiza ou cria a carteira do doador
            CashbackWallet wallet = cashbackWalletRepository.findByUserId(donor.getId())
                    .orElseGet(() -> {
                        CashbackWallet newWallet = new CashbackWallet();
                        newWallet.setUser(donor);
                        newWallet.setBalance(BigDecimal.ZERO);
                        return cashbackWalletRepository.save(newWallet);
                    });

            wallet.setBalance(wallet.getBalance().add(cashbackValue));
            cashbackWalletRepository.save(wallet);

            // 4. Cria o crédito e salva o snapshot de pointsPerKg
            CashbackCredit credit = new CashbackCredit();
            credit.setWalletId(wallet.getId());
            credit.setDescription("Cashback por doação de " + disposal.getApproximateWeightInKg() + "kg no ponto " + disposal.getCollectionPoint().getName());
            credit.setValue(cashbackValue);
            credit.setDateTimeTransaction(Instant.now());
            credit.setApprovedAt(Instant.now());
            credit.setStatus(TransactionStatus.CONFIRMED);
            credit.setDateTimeExpiration(Instant.now().plus(CASHBACK_EXPIRATION_DAYS, ChronoUnit.DAYS));
            credit.setCollectionPoint(disposal.getCollectionPoint());
            credit.setPointsPerKg(rule.getPointsPerKg()); // Atribui a regra aplicada

            credit = transactionRepository.save(credit);
            disposal.setCashbackCredit(credit);
        }

        return mapToDTO(disposalRepository.save(disposal));
    }

    // Rejeição do descarte (quando o doador não aparece ou dados falsos)
    @Transactional
    public DisposalResponseDTO rejectDisposal(Long disposalId, String approverUsername, String justification) {
        Disposal disposal = disposalRepository.findById(disposalId)
                .orElseThrow(() -> new EntityNotFoundException("Descarte não encontrado."));

        if (disposal.getStatus() != DisposalStatus.PENDING) {
            throw new IllegalStateException("Apenas descartes pendentes podem ser rejeitados.");
        }

        User approver = validatePointManagerOrOperator(disposal.getCollectionPoint().getId(), approverUsername);

        disposal.setStatus(DisposalStatus.REJECTED);
        disposal.setConfirmedBy(approver);
        disposal.setConfirmedAt(Instant.now());
        disposal.setJustification(justification);

        return mapToDTO(disposalRepository.save(disposal));
    }

    @Transactional(readOnly = true)
    public CollectionPointDisposalsSummaryDTO getDisposalsByCollectionPoint(Long collectionPointId, String username, Pageable pageable) {
        validatePointManagerOrOperator(collectionPointId, username);

        Page<DisposalResponseDTO> disposalsPage = disposalRepository.findByCollectionPointId(collectionPointId, pageable).map(this::mapToDTO);
        BigDecimal totalWeight = disposalRepository.getTotalWeightByCollectionPointId(collectionPointId);
        BigDecimal totalPoints = disposalRepository.getTotalEarnedPointsByCollectionPointId(collectionPointId);

        return new CollectionPointDisposalsSummaryDTO(totalWeight, totalPoints, disposalsPage);
    }


    // Isola a validação de segurança reutilizável (Retorna o Usuário caso validado com sucesso)
    private User validatePointManagerOrOperator(Long collectionPointId, String username) {
        User currentUser = userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        CollectionPoint point = collectionPointRepository.findById(collectionPointId)
                .orElseThrow(() -> new EntityNotFoundException("Ponto de coleta não encontrado."));

        boolean isAdmin = currentUser.getRole().contains(Role.ADMIN);
        boolean isManager = point.getManagers().stream().anyMatch(m -> m.getId().equals(currentUser.getId()));
        boolean isOperator = point.getOperators().stream().anyMatch(o -> o.getId().equals(currentUser.getId()));

        if (!isAdmin && !isManager && !isOperator) {
            throw new AccessDeniedException("Você não tem permissão para aceder aos descartes deste ponto de coleta.");
        }
        return currentUser;
    }

    private DisposalResponseDTO mapToDTO(Disposal entity) {
        boolean cashbackGenerated = entity.getCashbackCredit() != null;
        BigDecimal pointsPerKg = null;
        BigDecimal earnedPoints = BigDecimal.ZERO;

        if (cashbackGenerated) {
            // Remove o Proxy do Hibernate para acessar com segurança os atributos da subclasse CashbackCredit
            CashbackCredit credit = (CashbackCredit) Hibernate.unproxy(entity.getCashbackCredit());
            pointsPerKg = credit.getPointsPerKg();
            earnedPoints = credit.getValue();
        }

        return DisposalResponseDTO.builder()
                .id(entity.getId())
                .userName(entity.getUser().getName())
                .collectionPointName(entity.getCollectionPoint().getName())
                .approximateWeightInKg(entity.getApproximateWeightInKg())
                .status(entity.getStatus())
                .justification(entity.getJustification())
                .createdAt(entity.getCreatedAt())
                .generatedCashback(cashbackGenerated)
                .pointsPerKg(pointsPerKg)
                .earnedPoints(earnedPoints)
                .clothTypes(entity.getClothTypes().stream()
                        .map(ClothType::getName)
                        .collect(Collectors.toSet()))
                .build();
    }
}