package br.com.maisprati.projeto.service;

import br.com.maisprati.projeto.dto.request.UserChangePasswordRequestDTO;
import br.com.maisprati.projeto.dto.request.UserEditProfileRequestDTO;
import br.com.maisprati.projeto.dto.response.*;
import br.com.maisprati.projeto.model.entity.CollectionPoint;
import br.com.maisprati.projeto.model.entity.Post;
import br.com.maisprati.projeto.model.entity.User;
import br.com.maisprati.projeto.repository.CashbackWalletRepository;
import br.com.maisprati.projeto.repository.CollectionPointRepository;
import br.com.maisprati.projeto.repository.PostRepository;
import br.com.maisprati.projeto.repository.UserRepository;
import br.com.maisprati.projeto.util.PhoneUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CollectionPointRepository collectionPointRepository;
    private final CashbackWalletRepository cashbackWalletRepository;
    private final PasswordEncoder encoder;
    private final TokenService tokenService;
    private final UserValidationService userValidationService;
    private final CollectionPointService collectionPointService;
    private final FileStorageService fileStorageService;


    @Transactional
    public void changePassword(User loggedInUser, UserChangePasswordRequestDTO dto) {
        User user = userRepository.findById(loggedInUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado no banco de dados."));
        if (!encoder.matches(dto.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("A senha atual informada está incorreta.");
        }
        user.setEncodedPassword(dto.newPassword(), encoder);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserData(User loggedInUser) {
        User user = userRepository.findById(loggedInUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado no banco de dados."));
        return new UserResponseDTO(user);
    }

    @Transactional
    public UserUpdatedResponseDTO editUserData(User loggedInUser, UserEditProfileRequestDTO dto) {
        return editUserData(loggedInUser, dto, null);
    }

    @Transactional
    public UserUpdatedResponseDTO editUserData(
            User loggedInUser,
            UserEditProfileRequestDTO dto,
            MultipartFile image) {
        User user = userRepository.findById(loggedInUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado no banco de dados."));

        if (dto.removeAvatar() && image != null && !image.isEmpty()) {
            throw new IllegalArgumentException("Não é possível remover e enviar um novo avatar na mesma operação.");
        }

        if (dto.email() != null) {
            userValidationService.validateEmailUpdate(dto.email(), user.getEmail());
            user.setEmail(dto.email().toLowerCase());
        }
        if (dto.name() != null && !dto.name().isBlank())
            user.setName(dto.name().toUpperCase());
        if (dto.phone() != null)
            user.setPhone(PhoneUtils.formatToE164(dto.phone()));

        String previousAvatarPath = user.getAvatarUrl();
        if (image != null && !image.isEmpty()) {
            user.setAvatarUrl(fileStorageService.saveUserAvatar(user.getId(), image));
        } else if (dto.removeAvatar()) {
            user.setAvatarUrl(null);
        }

        userRepository.saveAndFlush(user);
        if (previousAvatarPath != null
                && !previousAvatarPath.equals(user.getAvatarUrl())) {
            fileStorageService.deleteImage(previousAvatarPath);
        }
        String newToken = tokenService.generateToken(user);

        return new UserUpdatedResponseDTO(new UserResponseDTO(user), newToken);
    }

    @Transactional(readOnly = true)
    public Resource getAvatar(User loggedInUser) {
        User user = userRepository.findById(loggedInUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado no banco de dados."));

        Resource avatar = fileStorageService.loadImage(user.getAvatarUrl());
        if (avatar == null) {
            throw new IllegalArgumentException("Usuário não possui imagem de perfil.");
        }
        return avatar;
    }

    @Transactional(readOnly = true)
    public Resource getAvatarForAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado no banco de dados."));

        Resource avatar = fileStorageService.loadImage(user.getAvatarUrl());
        if (avatar == null) {
            throw new IllegalArgumentException("Usuário não possui imagem de perfil.");
        }
        return avatar;
    }

    @Transactional(readOnly = true)
    public UserAssociationsResponseDTO getUserAssociations(User loggedInUser) {
        User user = userRepository.findById(loggedInUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado no banco de dados."));

        var managedCollectionPoints = collectionPointService.getCollectionPointsByUserManager(user);
        var operatedCollectionPoints = collectionPointService.getCollectionPointsByUserOperator(user);

        return new UserAssociationsResponseDTO(loggedInUser.getId(), managedCollectionPoints, operatedCollectionPoints);
    }


    @Transactional(readOnly = true)
    public Page<PostSummaryResponseDTO> getFavoritePosts(String email, Pageable pageable) {
        return userRepository.findFavoritePostsByUserEmail(email, pageable).map(PostSummaryResponseDTO::new);
    }

    @Transactional
    public void favoritePost(String username, Long postId) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post não encontrado."));
        if (user.getFavoritePosts().add(post)) {
            post.incrementFavoriteCount();
        }
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public void unfavoritePost(String username, Long postId) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
        boolean removed = user.getFavoritePosts().removeIf(p -> p.getId().equals(postId));
        if (removed) {
            postRepository.findById(postId).ifPresent(Post::decrementFavoriteCount);
        }
        userRepository.saveAndFlush(user);
    }

    @Transactional(readOnly = true)
    public Long getFavoritePostsCount(String email) {
        return userRepository.countFavoritePosts(email);
    }

    @Transactional(readOnly = true)
    public Boolean checkFavoritePost(String username, Long postId) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
        return user.getFavoritePosts().stream().anyMatch(p -> p.getId().equals(postId));
    }

    @Transactional(readOnly = true)
    public Page<CollectionPointSummaryResponseDTO> getFavoriteCollectionPoints(String email, Pageable pageable) {
        Page<CollectionPoint> favorites = userRepository.findFavoriteCollectionPointsByEmail(email, pageable);
        return collectionPointService.bindClothTypesToSummaryResponseDTO(favorites);
    }

    @Transactional
    public void favoriteCollectionPoint(String username, Long collectionPointId) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
        CollectionPoint collectionPoint = collectionPointRepository.findById(collectionPointId)
                .orElseThrow(() -> new EntityNotFoundException("Ponto de coleta não encontrado."));
        if (user.getFavoriteCollectionPoints().add(collectionPoint)){
            collectionPoint.incrementFavoriteCount();
        }
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public void unfavoriteCollectionPoint(String username, Long collectionPointId) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
        CollectionPoint collectionPoint = collectionPointRepository.findById(collectionPointId)
                .orElseThrow(() -> new EntityNotFoundException("Ponto de coleta não encontrado."));
        boolean removed = user.getFavoriteCollectionPoints().removeIf(cp -> cp.getId().equals(collectionPointId));
        if (removed) {
            collectionPoint.decrementFavoriteCount();
        }
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public Long getFavoriteCollectionPointsCount(String username) {
        return userRepository.countFavoriteCollectionPoints(username);
    }
}
