package br.com.maisprati.ecotecidos.service;

import br.com.maisprati.ecotecidos.dto.projection.CollectionPointDistanceProjectionDTO;
import br.com.maisprati.ecotecidos.dto.request.CollectionPointUpdateRequestDTO;
import br.com.maisprati.ecotecidos.model.entity.Address;
import br.com.maisprati.ecotecidos.model.entity.CollectionPoint;
import br.com.maisprati.ecotecidos.model.entity.CollectionPointPendingUpdate;
import br.com.maisprati.ecotecidos.model.entity.User;
import br.com.maisprati.ecotecidos.model.enums.CollectionPointStatus;
import br.com.maisprati.ecotecidos.model.enums.Role;
import br.com.maisprati.ecotecidos.repository.ClothTypeRepository;
import br.com.maisprati.ecotecidos.repository.CollectionPointRepository;
import br.com.maisprati.ecotecidos.repository.CollectionPointPendingUpdateRepository;
import br.com.maisprati.ecotecidos.repository.UserRepository;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionPointServiceTest {

    @Mock
    private CollectionPointRepository collectionPointRepository;

    @Mock
    private CollectionPointPendingUpdateRepository pendingUpdateRepository;

    @Mock
    private ClothTypeRepository clothTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegisterUserService registerUserService;

    @Mock
    private UserValidationService userValidationService;

    @Mock
    private br.com.maisprati.ecotecidos.mapper.CollectionPointMapper collectionPointMapper;

    @Mock
    private FileStorageService fileStorageService;

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
        ).findNearbyNoClothTypes(
                any(),
                any(),
                any(),
                any()
        );

        verify(collectionPointRepository, never()).findNearbyWithClothTypes(
                any(), any(), any(), any(), any()
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

        when(collectionPointRepository.findNearbyNoClothTypes(
                latitude,
                longitude,
                defaultRadius,
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

        verify(collectionPointRepository).findNearbyNoClothTypes(
                latitude,
                longitude,
                defaultRadius,
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

        when(collectionPointRepository.findNearbyWithClothTypes(
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

        verify(collectionPointRepository).findNearbyWithClothTypes(
                latitude,
                longitude,
                repositoryRadius,
                clothTypeIds,
                pageable
        );
    }

    // TEST 14 - Updates are submitted for approval, not applied immediately.
    @Test
    void shouldCreatePendingUpdateWithoutApplyingChanges() {
        CollectionPoint point = createCollectionPoint(12L, CollectionPointStatus.ACTIVE);
        User admin = createUser(1L, "admin@qa.com", Role.ADMIN);
        String originalName = point.getName();
        CollectionPointUpdateRequestDTO dto = new CollectionPointUpdateRequestDTO(
                "UPDATED ECOPONTO", null, null, null, null, null
        );

        when(collectionPointRepository.findById(12L)).thenReturn(Optional.of(point));
        when(userRepository.findByEmail("admin@qa.com")).thenReturn(Optional.of(admin));
        when(pendingUpdateRepository.findByCollectionPointId(12L)).thenReturn(Optional.empty());
        when(collectionPointRepository.save(point)).thenReturn(point);

        collectionPointService.updateCollectionPoint(12L, dto, "admin@qa.com");

        assertEquals(originalName, point.getName());
        verify(pendingUpdateRepository).save(any(CollectionPointPendingUpdate.class));
        verify(collectionPointRepository).save(point);
        verify(collectionPointMapper, never()).updateEntityFromDto(
                any(CollectionPointUpdateRequestDTO.class), any(CollectionPoint.class)
        );
    }

    @Test
    void shouldPersistUploadedImagePathInPendingUpdatePayload() throws Exception {
        CollectionPoint point = createCollectionPoint(13L, CollectionPointStatus.ACTIVE);
        User admin = createUser(1L, "admin@qa.com", Role.ADMIN);
        CollectionPointUpdateRequestDTO dto = new CollectionPointUpdateRequestDTO(
                "UPDATED ECOPONTO", null, null, null, null, null
        );
        MockMultipartFile image = new MockMultipartFile(
                "image", "point.jpg", "image/jpeg", new byte[] { 1, 2, 3 }
        );

        when(collectionPointRepository.findById(13L)).thenReturn(Optional.of(point));
        when(userRepository.findByEmail("admin@qa.com")).thenReturn(Optional.of(admin));
        when(pendingUpdateRepository.findByCollectionPointId(13L)).thenReturn(Optional.empty());
        when(collectionPointRepository.save(point)).thenReturn(point);
        when(fileStorageService.saveCollectionPointImage(13L, image))
                .thenReturn("collection-points/13/image_123.jpg");

        collectionPointService.updateCollectionPoint(13L, dto, image, "admin@qa.com");

        var pendingCaptor = org.mockito.ArgumentCaptor.forClass(CollectionPointPendingUpdate.class);
        verify(pendingUpdateRepository).save(pendingCaptor.capture());
        assertEquals(
                "collection-points/13/image_123.jpg",
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(pendingCaptor.getValue().getUpdatePayloadJson())
                        .get("imageUrl")
                        .asText()
        );
        verify(fileStorageService).saveCollectionPointImage(eq(13L), eq(image));
    }

    // TEST 15
    @Test
    void shouldRejectUpdateWhenCollectionPointDoesNotExist() {

        Long nonexistentCollectionPointId = 999L;

        CollectionPointUpdateRequestDTO dto =
                new CollectionPointUpdateRequestDTO(
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
                                        dto,
                                        "admin@qa.com"
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
                any(CollectionPointUpdateRequestDTO.class),
                any(CollectionPoint.class)
        );

        verify(
                collectionPointRepository,
                never()
        ).save(any(CollectionPoint.class));
    }

    // TEST 16 - A donor cannot request changes to a collection point they do not manage.
    @Test
    void shouldRejectUpdateForUnauthorizedUser() {
        CollectionPoint point = createCollectionPoint(16L, CollectionPointStatus.ACTIVE);
        User donor = createUser(99L, "donor@qa.com", Role.DOADOR);
        String originalName = point.getName();
        CollectionPointUpdateRequestDTO dto = new CollectionPointUpdateRequestDTO(
                "UNAUTHORIZED UPDATE", null, null, null, null, null
        );

        when(collectionPointRepository.findById(16L)).thenReturn(Optional.of(point));
        when(userRepository.findByEmail("donor@qa.com")).thenReturn(Optional.of(donor));

        assertThrows(
                AccessDeniedException.class,
                () -> collectionPointService.updateCollectionPoint(16L, dto, "donor@qa.com")
        );

        assertEquals(originalName, point.getName());
        verify(collectionPointRepository, never()).save(any(CollectionPoint.class));
        verify(pendingUpdateRepository, never()).save(any(CollectionPointPendingUpdate.class));
    }

    // TEST 17 - An approved pending update applies the proposed name and clears the request.
    @Test
    void shouldApplyApprovedPendingUpdateAndClearPendingFlag() {
        CollectionPoint point = createCollectionPoint(17L, CollectionPointStatus.ACTIVE);
        point.setHasPendingUpdate(true);
        String originalName = point.getName();
        String proposedName = "UPDATED ECOPONTO";
        CollectionPointPendingUpdate pending = CollectionPointPendingUpdate.builder()
                .collectionPoint(point)
                .updatePayloadJson("{\"name\":\"UPDATED ECOPONTO\"}")
                .build();

        when(pendingUpdateRepository.findByCollectionPointId(17L))
                .thenReturn(Optional.of(pending));
        doAnswer(invocation -> {
            CollectionPointUpdateRequestDTO proposed = invocation.getArgument(0);
            CollectionPoint target = invocation.getArgument(1);
            target.setName(proposed.name());
            return null;
        }).when(collectionPointMapper).updateEntityFromDto(
                any(CollectionPointUpdateRequestDTO.class), any(CollectionPoint.class));

        assertEquals(originalName, point.getName());
        collectionPointService.approvePendingUpdate(17L);

        assertEquals(proposedName, point.getName());
        assertEquals(false, ReflectionTestUtils.getField(point, "hasPendingUpdate"));
        verify(collectionPointRepository).save(point);
        verify(pendingUpdateRepository).delete(pending);
        verify(collectionPointMapper).updateEntityFromDto(
                any(CollectionPointUpdateRequestDTO.class), any(CollectionPoint.class));
    }

    // TEST 18 - Rejecting a pending update preserves the approved data.
    @Test
    void shouldRejectPendingUpdateWithoutChangingCollectionPoint() {
        CollectionPoint point = createCollectionPoint(18L, CollectionPointStatus.ACTIVE);
        point.setHasPendingUpdate(true);
        String originalName = point.getName();
        CollectionPointPendingUpdate pending = CollectionPointPendingUpdate.builder()
                .collectionPoint(point)
                .updatePayloadJson("{\"name\":\"REJECTED ECOPONTO\"}")
                .build();

        when(pendingUpdateRepository.findByCollectionPointId(18L))
                .thenReturn(Optional.of(pending));

        collectionPointService.rejectPendingUpdate(18L);

        assertEquals(originalName, point.getName());
        assertEquals(CollectionPointStatus.ACTIVE, point.getStatus());
        assertEquals(false, ReflectionTestUtils.getField(point, "hasPendingUpdate"));
        verify(collectionPointRepository).save(point);
        verify(pendingUpdateRepository).delete(pending);
        verify(collectionPointMapper, never()).updateEntityFromDto(
                any(CollectionPointUpdateRequestDTO.class), any(CollectionPoint.class));
    }

    // TEST 19 - Approving a nonexistent pending update must not change data.
    @Test
    void shouldRejectApprovalWhenPendingUpdateDoesNotExist() {
        Long collectionPointId = 19L;
        when(pendingUpdateRepository.findByCollectionPointId(collectionPointId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> collectionPointService.approvePendingUpdate(collectionPointId));

        verify(pendingUpdateRepository).findByCollectionPointId(collectionPointId);
        verify(collectionPointRepository, never()).save(any(CollectionPoint.class));
        verify(pendingUpdateRepository, never()).delete(any(CollectionPointPendingUpdate.class));
    }

    // TEST 20 - Rejecting a nonexistent pending update must not change data.
    @Test
    void shouldRejectRejectionWhenPendingUpdateDoesNotExist() {
        Long collectionPointId = 20L;
        when(pendingUpdateRepository.findByCollectionPointId(collectionPointId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> collectionPointService.rejectPendingUpdate(collectionPointId));

        verify(pendingUpdateRepository).findByCollectionPointId(collectionPointId);
        verify(collectionPointRepository, never()).save(any(CollectionPoint.class));
        verify(pendingUpdateRepository, never()).delete(any(CollectionPointPendingUpdate.class));
    }

    // TEST 21 - Malformed pending update JSON must not modify the collection point.
    @Test
    void shouldRejectApprovalWhenPendingUpdatePayloadIsInvalid() {
        CollectionPoint point = createCollectionPoint(21L, CollectionPointStatus.ACTIVE);
        point.setHasPendingUpdate(true);
        String originalName = point.getName();
        CollectionPointPendingUpdate pending = CollectionPointPendingUpdate.builder()
                .collectionPoint(point)
                .updatePayloadJson("{invalid-json")
                .build();

        when(pendingUpdateRepository.findByCollectionPointId(21L))
                .thenReturn(Optional.of(pending));

        assertThrows(RuntimeException.class,
                () -> collectionPointService.approvePendingUpdate(21L));

        assertEquals(originalName, point.getName());
        assertEquals(true, ReflectionTestUtils.getField(point, "hasPendingUpdate"));
        verify(collectionPointRepository, never()).save(any(CollectionPoint.class));
        verify(pendingUpdateRepository, never()).delete(any(CollectionPointPendingUpdate.class));
        verify(collectionPointMapper, never()).updateEntityFromDto(
                any(CollectionPointUpdateRequestDTO.class), any(CollectionPoint.class));
    }

    // TEST 22 - A nonexistent collection point cannot be retrieved as active.
    @Test
    void shouldRejectActiveCollectionPointLookupWhenPointDoesNotExist() {
        Long collectionPointId = 22L;
        when(collectionPointRepository.findById(collectionPointId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> collectionPointService.getCollectionPointByIdAndStatusActive(collectionPointId));

        verify(collectionPointRepository).findById(collectionPointId);
        verify(collectionPointRepository, never()).save(any(CollectionPoint.class));
    }

    // TEST 23 - A suspended collection point cannot be retrieved as active.
    @Test
    void shouldRejectSuspendedCollectionPointWhenActiveStatusIsRequired() {
        Long collectionPointId = 23L;
        CollectionPoint point = createCollectionPoint(collectionPointId, CollectionPointStatus.SUSPENDED);

        when(collectionPointRepository.findById(collectionPointId))
                .thenReturn(Optional.of(point));

        assertThrows(IllegalArgumentException.class,
                () -> collectionPointService.getCollectionPointByIdAndStatusActive(collectionPointId));

        assertEquals(CollectionPointStatus.SUSPENDED, point.getStatus());
        verify(collectionPointRepository).findById(collectionPointId);
        verify(collectionPointRepository, never()).save(any(CollectionPoint.class));
    }

    // TEST 24 - A collection point without a status cannot be retrieved as active.
    @Test
    void shouldRejectCollectionPointWithoutStatusWhenActiveStatusIsRequired() {
        Long collectionPointId = 24L;
        CollectionPoint point = createCollectionPoint(collectionPointId, null);

        when(collectionPointRepository.findById(collectionPointId))
                .thenReturn(Optional.of(point));

        assertThrows(IllegalArgumentException.class,
                () -> collectionPointService.getCollectionPointByIdAndStatusActive(collectionPointId));

        assertNull(point.getStatus());
        verify(collectionPointRepository).findById(collectionPointId);
        verify(collectionPointRepository, never()).save(any(CollectionPoint.class));
    }

    // TEST 25 - A null collection point ID cannot be retrieved as active.
    @Test
    void shouldRejectActiveCollectionPointLookupWhenIdIsNull() {
        when(collectionPointRepository.findById(null))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> collectionPointService.getCollectionPointByIdAndStatusActive(null));

        verify(collectionPointRepository).findById(null);
        verify(collectionPointRepository, never()).save(any(CollectionPoint.class));
    }

    private Address createAddress() {

        return Address.builder()
                .street("QA STREET")
                .number("100")
                .neighborhood("CENTRO")
                .city("PORTO ALEGRE")
                .state(
                        br.com.maisprati.ecotecidos.model.enums.State.RS
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
