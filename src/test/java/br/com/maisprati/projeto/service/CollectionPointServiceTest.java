package br.com.maisprati.projeto.service;

import br.com.maisprati.projeto.dto.projection.CollectionPointDistanceProjectionDTO;
import br.com.maisprati.projeto.dto.request.CollectionPointUpdateDTO;
import br.com.maisprati.projeto.model.entity.Address;
import br.com.maisprati.projeto.model.entity.CollectionPoint;
import br.com.maisprati.projeto.model.entity.User;
import br.com.maisprati.projeto.model.enums.CollectionPointStatus;
import br.com.maisprati.projeto.model.enums.Role;
import br.com.maisprati.projeto.repository.ClothTypeRepository;
import br.com.maisprati.projeto.repository.CollectionPointRepository;
import br.com.maisprati.projeto.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionPointServiceTest {

    @Mock
    private CollectionPointRepository collectionPointRepository;

    @Mock
    private ClothTypeRepository clothTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegisterUserService registerUserService;

    @Mock
    private UserValidationService userValidationService;

    @Mock
    private br.com.maisprati.projeto.mapper.CollectionPointMapper collectionPointMapper;

    @InjectMocks
    private CollectionPointService collectionPointService;

    // TEST 1
    @Test
    void shouldApprovePendingCollectionPoint() {

        CollectionPoint point = createCollectionPoint(
                2L,
                CollectionPointStatus.PENDING
        );

        when(collectionPointRepository.findById(2L))
                .thenReturn(Optional.of(point));

        when(collectionPointRepository.save(point))
                .thenReturn(point);

        var result =
                collectionPointService.approveCollectionPoint(2L);

        assertEquals(
                CollectionPointStatus.ACTIVE,
                point.getStatus()
        );

        assertEquals(
                CollectionPointStatus.ACTIVE,
                result.getStatus()
        );

        verify(collectionPointRepository).findById(2L);
        verify(collectionPointRepository).save(point);
    }

    // TEST 2
    @Test
    void shouldRejectPendingCollectionPoint() {

        CollectionPoint point = createCollectionPoint(
                3L,
                CollectionPointStatus.PENDING
        );

        when(collectionPointRepository.findById(3L))
                .thenReturn(Optional.of(point));

        when(collectionPointRepository.save(point))
                .thenReturn(point);

        var result =
                collectionPointService.rejectCollectionPoint(3L);

        assertEquals(
                CollectionPointStatus.SUSPENDED,
                point.getStatus()
        );

        assertEquals(
                CollectionPointStatus.SUSPENDED,
                result.getStatus()
        );

        verify(collectionPointRepository).findById(3L);
        verify(collectionPointRepository).save(point);
    }

    // TEST 3
    @Test
    void shouldRejectPauseForUnauthorizedUser() {

        CollectionPoint point = createCollectionPoint(
                4L,
                CollectionPointStatus.ACTIVE
        );

        User donor = createUser(
                99L,
                "doador@qa.com",
                Role.DOADOR
        );

        when(collectionPointRepository.findById(4L))
                .thenReturn(Optional.of(point));

        when(userRepository.findByEmail("doador@qa.com"))
                .thenReturn(Optional.of(donor));

        assertThrows(
                AccessDeniedException.class,
                () -> collectionPointService.pauseCollectionPoint(
                        4L,
                        "doador@qa.com"
                )
        );

        assertEquals(
                CollectionPointStatus.ACTIVE,
                point.getStatus()
        );

        verify(collectionPointRepository).findById(4L);
        verify(userRepository).findByEmail("doador@qa.com");

        verify(
                collectionPointRepository,
                never()
        ).save(point);
    }

    // TEST 4
    @Test
    void shouldAllowAdminToPauseActiveCollectionPoint() {

        CollectionPoint point = createCollectionPoint(
                5L,
                CollectionPointStatus.ACTIVE
        );

        User admin = createUser(
                1L,
                "admin@qa.com",
                Role.ADMIN
        );

        when(collectionPointRepository.findById(5L))
                .thenReturn(Optional.of(point));

        when(userRepository.findByEmail("admin@qa.com"))
                .thenReturn(Optional.of(admin));

        when(collectionPointRepository.save(point))
                .thenReturn(point);

        var result =
                collectionPointService.pauseCollectionPoint(
                        5L,
                        "admin@qa.com"
                );

        assertEquals(
                CollectionPointStatus.PAUSED,
                point.getStatus()
        );

        assertEquals(
                CollectionPointStatus.PAUSED,
                result.getStatus()
        );

        verify(collectionPointRepository).findById(5L);
        verify(userRepository).findByEmail("admin@qa.com");
        verify(collectionPointRepository).save(point);
    }

    // TEST 5
    @Test
    void shouldAllowManagerToPauseOwnCollectionPoint() {

        User manager = createUser(
                10L,
                "manager@qa.com",
                Role.PONTO_COLETA_GERENTE
        );

        CollectionPoint point = createCollectionPoint(
                6L,
                CollectionPointStatus.ACTIVE
        );

        point.getManagers().add(manager);

        when(collectionPointRepository.findById(6L))
                .thenReturn(Optional.of(point));

        when(userRepository.findByEmail("manager@qa.com"))
                .thenReturn(Optional.of(manager));

        when(collectionPointRepository.save(point))
                .thenReturn(point);

        var result =
                collectionPointService.pauseCollectionPoint(
                        6L,
                        "manager@qa.com"
                );

        assertEquals(
                CollectionPointStatus.PAUSED,
                point.getStatus()
        );

        assertEquals(
                CollectionPointStatus.PAUSED,
                result.getStatus()
        );

        verify(collectionPointRepository).findById(6L);
        verify(userRepository).findByEmail("manager@qa.com");
        verify(collectionPointRepository).save(point);
    }

    // TEST 6
    @Test
    void shouldRejectPauseForManagerFromAnotherCollectionPoint() {

        User actualManager = createUser(
                10L,
                "actual.manager@qa.com",
                Role.PONTO_COLETA_GERENTE
        );

        User anotherManager = createUser(
                20L,
                "another.manager@qa.com",
                Role.PONTO_COLETA_GERENTE
        );

        CollectionPoint point = createCollectionPoint(
                7L,
                CollectionPointStatus.ACTIVE
        );

        point.getManagers().add(actualManager);

        when(collectionPointRepository.findById(7L))
                .thenReturn(Optional.of(point));

        when(userRepository.findByEmail("another.manager@qa.com"))
                .thenReturn(Optional.of(anotherManager));

        assertThrows(
                AccessDeniedException.class,
                () -> collectionPointService.pauseCollectionPoint(
                        7L,
                        "another.manager@qa.com"
                )
        );

        assertEquals(
                CollectionPointStatus.ACTIVE,
                point.getStatus()
        );

        verify(collectionPointRepository).findById(7L);

        verify(userRepository)
                .findByEmail("another.manager@qa.com");

        verify(
                collectionPointRepository,
                never()
        ).save(point);
    }

    // TEST 7
    @Test
    void shouldReturnActiveCollectionPointById() {

        CollectionPoint point = createCollectionPoint(
                8L,
                CollectionPointStatus.ACTIVE
        );

        when(collectionPointRepository.findById(8L))
                .thenReturn(Optional.of(point));

        var result =
                collectionPointService
                        .getCollectionPointByIdAndStatusActive(8L);

        assertEquals(8L, result.getId());
        assertEquals("ECOPONTO QA", result.getName());

        assertEquals(
                CollectionPointStatus.ACTIVE,
                result.getStatus()
        );

        verify(collectionPointRepository).findById(8L);
    }

    // TEST 8
    @Test
    void shouldRejectPendingCollectionPointWhenActiveStatusIsRequired() {

        CollectionPoint point = createCollectionPoint(
                9L,
                CollectionPointStatus.PENDING
        );

        when(collectionPointRepository.findById(9L))
                .thenReturn(Optional.of(point));

        assertThrows(
                IllegalArgumentException.class,
                () -> collectionPointService
                        .getCollectionPointByIdAndStatusActive(9L)
        );

        assertEquals(
                CollectionPointStatus.PENDING,
                point.getStatus()
        );

        verify(collectionPointRepository).findById(9L);

        verify(
                collectionPointRepository,
                never()
        ).save(point);
    }

    // TEST 9
    @Test
    void shouldRejectPausedCollectionPointWhenActiveStatusIsRequired() {

        CollectionPoint point = createCollectionPoint(
                10L,
                CollectionPointStatus.PAUSED
        );

        when(collectionPointRepository.findById(10L))
                .thenReturn(Optional.of(point));

        assertThrows(
                IllegalArgumentException.class,
                () -> collectionPointService
                        .getCollectionPointByIdAndStatusActive(10L)
        );

        assertEquals(
                CollectionPointStatus.PAUSED,
                point.getStatus()
        );

        verify(collectionPointRepository).findById(10L);

        verify(
                collectionPointRepository,
                never()
        ).save(point);
    }

    // TEST 10
    @Test
    void shouldReturnOnlyActiveCollectionPointsInPublicListing() {

        var pageable = PageRequest.of(0, 10);

        CollectionPoint activePoint = createCollectionPoint(
                11L,
                CollectionPointStatus.ACTIVE
        );

        Page<CollectionPoint> activePoints =
                new PageImpl<>(
                        List.of(activePoint),
                        pageable,
                        1
                );

        when(collectionPointRepository.findByStatus(
                CollectionPointStatus.ACTIVE,
                pageable
        )).thenReturn(activePoints);

        var result =
                collectionPointService
                        .findPublicApprovedPoints(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        assertEquals(
                11L,
                result.getContent().get(0).getId()
        );

        assertEquals(
                CollectionPointStatus.ACTIVE,
                result.getContent().get(0).getStatus()
        );

        verify(collectionPointRepository).findByStatus(
                CollectionPointStatus.ACTIVE,
                pageable
        );
    }

    // TEST 11
    @Test
    void shouldRejectInvalidGeographicCoordinates() {

        var pageable = PageRequest.of(0, 10);

        BigDecimal invalidLatitude =
                new BigDecimal("91");

        BigDecimal validLongitude =
                new BigDecimal("-51.2300");

        var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> collectionPointService.findNearby(
                                invalidLatitude,
                                validLongitude,
                                null,
                                null,
                                pageable
                        )
                );

        assertEquals(
                "Coordenadas geográficas inválidas.",
                exception.getMessage()
        );

        verify(
                collectionPointRepository,
                never()
        ).findNearby(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    // TEST 12
    @Test
    void shouldUseDefaultRadiusWhenRadiusIsNotProvided() {

        var pageable = PageRequest.of(0, 10);

        BigDecimal latitude =
                new BigDecimal("-30.0346");

        BigDecimal longitude =
                new BigDecimal("-51.2177");

        BigDecimal defaultRadius =
                new BigDecimal("15.0");

        Page<CollectionPointDistanceProjectionDTO> emptyPage =
                new PageImpl<>(
                        List.of(),
                        pageable,
                        0
                );

        when(collectionPointRepository.findNearby(
                latitude,
                longitude,
                defaultRadius,
                null,
                pageable
        )).thenReturn(emptyPage);

        var result =
                collectionPointService.findNearby(
                        latitude,
                        longitude,
                        null,
                        null,
                        pageable
                );

        assertEquals(
                0,
                result.getTotalElements()
        );

        verify(collectionPointRepository).findNearby(
                latitude,
                longitude,
                defaultRadius,
                null,
                pageable
        );
    }

    // TEST 13
    @Test
    void shouldSearchNearbyCollectionPointsUsingClothTypeFilter() {

        var pageable = PageRequest.of(0, 10);

        BigDecimal latitude =
                new BigDecimal("-30.0346");

        BigDecimal longitude =
                new BigDecimal("-51.2177");

        Double radius = 10.0;

        BigDecimal repositoryRadius =
                new BigDecimal("10");

        List<Long> clothTypeIds =
                List.of(1L, 2L);

        Page<CollectionPointDistanceProjectionDTO> emptyPage =
                new PageImpl<>(
                        List.of(),
                        pageable,
                        0
                );

        when(collectionPointRepository.findNearby(
                latitude,
                longitude,
                repositoryRadius,
                clothTypeIds,
                pageable
        )).thenReturn(emptyPage);

        var result =
                collectionPointService.findNearby(
                        latitude,
                        longitude,
                        radius,
                        clothTypeIds,
                        pageable
                );

        assertEquals(
                0,
                result.getTotalElements()
        );

        verify(collectionPointRepository).findNearby(
                latitude,
                longitude,
                repositoryRadius,
                clothTypeIds,
                pageable
        );
    }

    // TEST 14
    @Test
    void shouldRejectUpdateWhenClothTypeDoesNotExist() {

        CollectionPoint point = createCollectionPoint(
                12L,
                CollectionPointStatus.ACTIVE
        );

        Set<Long> requestedClothTypeIds =
                Set.of(999L);

        CollectionPointUpdateDTO dto =
                new CollectionPointUpdateDTO(
                        null,
                        null,
                        null,
                        null,
                        requestedClothTypeIds,
                        null
                );

        when(collectionPointRepository.findById(12L))
                .thenReturn(Optional.of(point));

        when(clothTypeRepository.findAllById(requestedClothTypeIds))
                .thenReturn(List.of());

        var exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> collectionPointService
                                .updateCollectionPoint(
                                        12L,
                                        dto
                                )
                );

        assertEquals(
                "Um ou mais tipos de tecidos informados não foram encontrados.",
                exception.getMessage()
        );

        verify(collectionPointRepository).findById(12L);

        verify(clothTypeRepository)
                .findAllById(requestedClothTypeIds);

        verify(
                collectionPointRepository,
                never()
        ).save(any(CollectionPoint.class));
    }

    // TEST 15
    @Test
    void shouldRejectUpdateWhenCollectionPointDoesNotExist() {

        Long nonexistentCollectionPointId = 999L;

        CollectionPointUpdateDTO dto =
                new CollectionPointUpdateDTO(
                        "ECOPONTO INEXISTENTE",
                        null,
                        null,
                        null,
                        null,
                        null
                );

        when(collectionPointRepository.findById(
                nonexistentCollectionPointId
        )).thenReturn(Optional.empty());

        var exception =
                assertThrows(
                        EntityNotFoundException.class,
                        () -> collectionPointService
                                .updateCollectionPoint(
                                        nonexistentCollectionPointId,
                                        dto
                                )
                );

        assertEquals(
                "Ponto de coleta não encontrado.",
                exception.getMessage()
        );

        verify(collectionPointRepository)
                .findById(nonexistentCollectionPointId);

        verify(
                collectionPointMapper,
                never()
        ).updateEntityFromDto(
                any(CollectionPointUpdateDTO.class),
                any(CollectionPoint.class)
        );

        verify(
                collectionPointRepository,
                never()
        ).save(any(CollectionPoint.class));
    }

    private Address createAddress() {

        return Address.builder()
                .street("QA STREET")
                .number("100")
                .neighborhood("CENTRO")
                .city("PORTO ALEGRE")
                .state(
                        br.com.maisprati.projeto.model.enums.State.RS
                )
                .country("BRASIL")
                .zipCode("90000000")
                .build();
    }

    private CollectionPoint createCollectionPoint(
            Long id,
            CollectionPointStatus status
    ) {

        return CollectionPoint.builder()
                .id(id)
                .name("ECOPONTO QA")
                .status(status)
                .address(createAddress())
                .clothTypes(new HashSet<>())
                .operatingHours(new HashSet<>())
                .managers(new HashSet<>())
                .build();
    }

    private User createUser(
            Long id,
            String email,
            Role role
    ) {

        User user = new User();

        user.setId(id);
        user.setEmail(email);
        user.setRole(Set.of(role));

        return user;
    }
}