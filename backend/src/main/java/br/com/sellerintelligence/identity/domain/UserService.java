package br.com.sellerintelligence.identity.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User resolveOrCreateUserFromJwt(Jwt jwt) {
        String sub = jwt.getSubject();
        Objects.requireNonNull(sub, "JWT sub must not be null");

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = sub + "@autoprovisioned.local";
        }

        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = jwt.getClaimAsString("preferred_username");
        }
        if (name == null || name.isBlank()) {
            name = email;
        }

        final String finalEmail = email;
        final String finalName = name;

        return userRepository.findByKeycloakSub(sub)
                .map(existingUser -> {
                    boolean updated = false;
                    if (!Objects.equals(existingUser.getEmail(), finalEmail)) {
                        existingUser.setEmail(finalEmail);
                        updated = true;
                    }
                    if (!Objects.equals(existingUser.getName(), finalName)) {
                        existingUser.setName(finalName);
                        updated = true;
                    }
                    if (updated) {
                        return userRepository.save(existingUser);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    log.info("Provisioning new local user for OIDC sub: {}", sub);
                    User newUser = new User(sub, finalEmail, finalName);
                    return userRepository.save(newUser);
                });
    }

    @Transactional(readOnly = true)
    public Optional<User> findByKeycloakSub(String sub) {
        return userRepository.findByKeycloakSub(sub);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
}
