package br.com.maisprati.projeto.service;

import br.com.maisprati.projeto.dto.request.UserChangePasswordRequestDTO;
import br.com.maisprati.projeto.dto.request.UserEditProfileRequestDTO;
import br.com.maisprati.projeto.dto.response.UserAssociationsResponseDTO;
import br.com.maisprati.projeto.dto.response.UserResponseDTO;
import br.com.maisprati.projeto.dto.response.UserUpdatedResponseDTO;
import br.com.maisprati.projeto.model.entity.User;
import br.com.maisprati.projeto.repository.UserRepository;
import br.com.maisprati.projeto.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
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

}
