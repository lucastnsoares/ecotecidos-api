package br.com.maisprati.ecotecidos.service;

import br.com.maisprati.ecotecidos.dto.request.AuthForgotPasswordRequestDTO;
import br.com.maisprati.ecotecidos.dto.request.UserCreateRequestDTO;
import br.com.maisprati.ecotecidos.dto.request.UserRegisterRequestDTO;
import br.com.maisprati.ecotecidos.dto.response.UserSummaryResponseDTO;
import br.com.maisprati.ecotecidos.dto.response.UserResponseDTO;
import br.com.maisprati.ecotecidos.model.entity.User;
import br.com.maisprati.ecotecidos.model.enums.Role;
import br.com.maisprati.ecotecidos.repository.UserRepository;
import br.com.maisprati.ecotecidos.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RegisterUserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenService passwordResetTokenService;
    private final UserValidationService userValidationService;

    @Transactional
    public UserSummaryResponseDTO registerUserPublic(UserRegisterRequestDTO dto) {
        userValidationService.validateNewUser(dto.email(), dto.document());

        User newUser = new User();
        newUser.setRole(Collections.singleton(Role.DOADOR));
        newUser.setName(dto.name().toUpperCase());
        newUser.setDocument(dto.document().replaceAll("[.\\-/]", "").trim().toUpperCase());
        newUser.setEmail(dto.email().toLowerCase());
        newUser.setEncodedPassword(dto.password(), passwordEncoder);
        newUser.setPhone(PhoneUtils.formatToE164(dto.phone()));

        userRepository.saveAndFlush(newUser);

        return new UserSummaryResponseDTO(newUser);
    }

    @Transactional
    public UserResponseDTO registerUserAdmin(UserCreateRequestDTO dto) {
        userValidationService.validateNewUser(dto.email(), dto.document());

        User newUser = new User();
        newUser.setRole(dto.roles());
        newUser.setName(dto.name().toUpperCase());
        newUser.setDocument(dto.document().replaceAll("[.\\-/]", "").trim().toUpperCase());
        newUser.setEmail(dto.email().toLowerCase());
        newUser.setPhone(PhoneUtils.formatToE164(dto.phone()));

        User savedUser = userRepository.saveAndFlush(newUser);

        passwordResetTokenService.forgotPassword(new AuthForgotPasswordRequestDTO(savedUser.getEmail()));

        return new UserResponseDTO(newUser, null);
    }
}
