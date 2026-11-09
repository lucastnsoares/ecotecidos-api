package br.com.maisprati.ecotecidos.config.seed;

import br.com.maisprati.ecotecidos.model.entity.User;
import br.com.maisprati.ecotecidos.model.enums.Role;
import br.com.maisprati.ecotecidos.repository.UserRepository;
import br.com.maisprati.ecotecidos.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@Profile({"dev", "prod"})
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.name:}")
    private String adminName;

    @Value("${app.seed.admin.document:}")
    private String adminDocument;

    @Value("${app.seed.admin.email:}")
    private String adminEmail;

    @Value("${app.seed.admin.phone:}")
    private String adminPhone;

    @Value("${app.seed.admin.password:}")
    private String adminPassword;

    @Value("${app.seed.doador.name:}")
    private String donorName;

    @Value("${app.seed.doador.document:}")
    private String donorDocument;

    @Value("${app.seed.doador.email:}")
    private String donorEmail;

    @Value("${app.seed.doador.phone:}")
    private String donorPhone;

    @Value("${app.seed.doador.password:}")
    private String donorPassword;

    @Override
    @Transactional
    public void run(String... args) {
        List<User> usersToSave = new ArrayList<>();

        // 1. Cria usuário Administrador inicial
        buildSeedUser("ADMIN", Role.ADMIN, adminName, adminDocument, adminEmail, adminPhone, adminPassword, usersToSave)
                .ifPresent(usersToSave::add);

        // 2. Cria usuário Doador inicial
        buildSeedUser("DOADOR", Role.DOADOR, donorName, donorDocument, donorEmail, donorPhone, donorPassword, usersToSave)
                .ifPresent(usersToSave::add);

        // 3. Salva no banco
        if (!usersToSave.isEmpty()) {
            userRepository.saveAll(usersToSave);
            log.info("Seed inicial concluído: {} usuário(s) criado(s) com sucesso.", usersToSave.size());
        }
    }

    private Optional<User> buildSeedUser(
            String label,
            Role role,
            String name,
            String document,
            String email,
            String phone,
            String password,
            List<User> pendingUsers
    ) {
        if (isBlank(name) && isBlank(document) && isBlank(email) && isBlank(password)) {
            log.debug("Seed do usuário {} ignorado (variáveis não definidas).", label);
            return Optional.empty();
        }

        if (isBlank(name) || isBlank(document) || isBlank(email) || isBlank(password)) {
            log.warn("Seed do usuário {} ignorado: informe nome, documento, e-mail e senha completos.", label);
            return Optional.empty();
        }

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedDocument = document.replaceAll("[.\\-/]", "").trim().toUpperCase();

        boolean emailExistsInDb = userRepository.existsByEmailIgnoreCase(normalizedEmail);
        boolean documentExistsInDb = userRepository.existsByDocument(normalizedDocument)
                || userRepository.existsByDocument(document.trim());

        boolean duplicateInBatch = pendingUsers.stream().anyMatch(u ->
                u.getEmail().equalsIgnoreCase(normalizedEmail) || u.getDocument().equalsIgnoreCase(normalizedDocument));

        if (emailExistsInDb || documentExistsInDb || duplicateInBatch) {
            log.info("Seed do usuário {} ignorado: já existe usuário com este e-mail ({}) ou documento ({}).",
                    label, normalizedEmail, normalizedDocument);
            return Optional.empty();
        }

        User user = new User();
        user.setName(name.trim().toUpperCase());
        user.setDocument(normalizedDocument);
        user.setEmail(normalizedEmail);
        user.setPhone(PhoneUtils.formatToE164(phone));
        user.setEncodedPassword(password, passwordEncoder);
        user.setRole(Set.of(role));
        user.setActive(true);

        return Optional.of(user);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
