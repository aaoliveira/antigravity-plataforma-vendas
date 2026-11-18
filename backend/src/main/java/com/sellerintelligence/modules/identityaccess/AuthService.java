package com.sellerintelligence.modules.identityaccess;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.correlation.CorrelationIdFilter;
import com.sellerintelligence.core.security.AuthenticatedUser;
import com.sellerintelligence.core.security.BruteForceBlockedException;
import com.sellerintelligence.core.security.BruteForceProtectionService;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.core.tenant.TenantAccessDeniedException;
import com.sellerintelligence.modules.identityaccess.dto.AuthResponse;
import com.sellerintelligence.modules.identityaccess.dto.LoginRequest;
import com.sellerintelligence.modules.identityaccess.dto.UserMembershipDto;
import com.sellerintelligence.modules.identityaccess.dto.UserProfileResponse;
import com.sellerintelligence.modules.tenancy.Membership;
import com.sellerintelligence.modules.tenancy.MembershipRepository;
import com.sellerintelligence.modules.tenancy.Organization;
import com.sellerintelligence.modules.tenancy.OrganizationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço de autenticação mínima, emissão de JWT, controle de força bruta e auditoria.
 */
@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final BruteForceProtectionService bruteForceProtectionService;
    private final AuditPublisher auditPublisher;
    private final long tokenExpirationSeconds;

    public AuthService(
            AppUserRepository userRepository,
            MembershipRepository membershipRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            BruteForceProtectionService bruteForceProtectionService,
            AuditPublisher auditPublisher,
            @Value("${security.jwt.expiration-seconds:900}") long tokenExpirationSeconds
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.bruteForceProtectionService = bruteForceProtectionService;
        this.auditPublisher = auditPublisher;
        this.tokenExpirationSeconds = tokenExpirationSeconds;
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        String bruteForceKey = clientIp + ":" + request.email().toLowerCase().trim();

        // 1. Verificação de proteção contra brute force
        if (bruteForceProtectionService.isBlocked(bruteForceKey)) {
            long remainingSeconds = bruteForceProtectionService.getRemainingLockoutSeconds(bruteForceKey);
            auditPublisher.publish(
                    "BRUTE_FORCE_BLOCKED",
                    null,
                    request.email(),
                    null,
                    clientIp,
                    userAgent,
                    correlationId,
                    "Tentativa de autenticação rejeitada por bloqueio de brute force. Tempo restante: " + remainingSeconds + "s"
            );
            throw new BruteForceBlockedException(
                    "Muitas tentativas consecutivas com falha. Acesso temporariamente bloqueado. Tente novamente em " + remainingSeconds + " segundos."
            );
        }

        // 2. Busca do usuário pelo e-mail
        Optional<AppUser> userOpt = userRepository.findByEmailIgnoreCase(request.email().trim());
        if (userOpt.isEmpty()) {
            bruteForceProtectionService.recordFailure(bruteForceKey);
            auditPublisher.publish(
                    "LOGIN_FAILED",
                    null,
                    request.email(),
                    null,
                    clientIp,
                    userAgent,
                    correlationId,
                    "Usuário não encontrado"
            );
            throw new BadCredentialsException("Credenciais inválidas. Verifique seu e-mail e senha.");
        }

        AppUser user = userOpt.get();

        // 3. Validação da senha com BCrypt
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            bruteForceProtectionService.recordFailure(bruteForceKey);
            auditPublisher.publish(
                    "LOGIN_FAILED",
                    user.getId(),
                    user.getEmail(),
                    null,
                    clientIp,
                    userAgent,
                    correlationId,
                    "Senha incorreta"
            );
            throw new BadCredentialsException("Credenciais inválidas. Verifique seu e-mail e senha.");
        }

        // 4. Verificação de status ativo da conta
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            auditPublisher.publish(
                    "LOGIN_FAILED",
                    user.getId(),
                    user.getEmail(),
                    null,
                    clientIp,
                    userAgent,
                    correlationId,
                    "Conta inativa ou suspensa"
            );
            throw new BadCredentialsException("Conta de usuário inativa ou suspensa. Contate o suporte.");
        }

        // 5. Sucesso na autenticação: zera tentativas no brute force
        bruteForceProtectionService.recordSuccess(bruteForceKey);

        // 6. Resolução de memberships e tenant ativo
        List<Membership> activeMemberships = membershipRepository.findByUserIdAndStatus(user.getId(), "ACTIVE");
        UUID activeTenantId = null;
        List<String> roles = new ArrayList<>();

        if (request.requestedTenantId() != null) {
            // Cliente solicitou um tenant específico no login: valida se ele pertence a ele
            Optional<Membership> matchedMembership = activeMemberships.stream()
                    .filter(m -> m.getTenantId().equals(request.requestedTenantId()))
                    .findFirst();

            if (matchedMembership.isEmpty()) {
                auditPublisher.publish(
                        "TENANT_SPOOFING_ATTEMPT",
                        user.getId(),
                        user.getEmail(),
                        request.requestedTenantId(),
                        clientIp,
                        userAgent,
                        correlationId,
                        "Tentativa de login solicitando tenant no qual o usuário não possui membership ativa"
                );
                throw new TenantAccessDeniedException("O usuário não possui membership ativa na organização solicitada.");
            }

            activeTenantId = matchedMembership.get().getTenantId();
            roles.add("ROLE_" + matchedMembership.get().getRole().toUpperCase());
        } else if (!activeMemberships.isEmpty()) {
            // Deriva o primeiro tenant ativo como padrão
            Membership primaryMembership = activeMemberships.get(0);
            activeTenantId = primaryMembership.getTenantId();
            roles.add("ROLE_" + primaryMembership.getRole().toUpperCase());
        } else {
            roles.add("ROLE_USER");
        }

        // 7. Geração do token JWT RFC 7519
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), activeTenantId, roles);

        // 8. Registro de auditoria de sucesso
        auditPublisher.publish(
                "LOGIN_SUCCESS",
                user.getId(),
                user.getEmail(),
                activeTenantId,
                clientIp,
                userAgent,
                correlationId,
                "Autenticação efetuada com sucesso"
        );

        return new AuthResponse(
                accessToken,
                "Bearer",
                tokenExpirationSeconds,
                activeTenantId,
                roles
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse switchTenant(UUID targetTenantId, AuthenticatedUser authenticatedUser, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);

        Optional<Membership> membershipOpt = membershipRepository
                .findByUserIdAndTenantIdAndStatus(authenticatedUser.getUserId(), targetTenantId, "ACTIVE");

        if (membershipOpt.isEmpty()) {
            auditPublisher.publish(
                    "TENANT_SPOOFING_ATTEMPT",
                    authenticatedUser.getUserId(),
                    authenticatedUser.getEmail(),
                    targetTenantId,
                    clientIp,
                    userAgent,
                    correlationId,
                    "Tentativa de alternar para organização sem membership ativa"
            );
            throw new TenantAccessDeniedException("Acesso negado: o usuário não possui membership ativa no tenant destino informado.");
        }

        Membership membership = membershipOpt.get();
        List<String> roles = List.of("ROLE_" + membership.getRole().toUpperCase());

        String newAccessToken = jwtTokenProvider.generateToken(
                authenticatedUser.getUserId(),
                authenticatedUser.getEmail(),
                targetTenantId,
                roles
        );

        auditPublisher.publish(
                "TENANT_SWITCH_SUCCESS",
                authenticatedUser.getUserId(),
                authenticatedUser.getEmail(),
                targetTenantId,
                clientIp,
                userAgent,
                correlationId,
                "Contexto de tenant alternado com sucesso"
        );

        return new AuthResponse(
                newAccessToken,
                "Bearer",
                tokenExpirationSeconds,
                targetTenantId,
                roles
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMe(AuthenticatedUser authenticatedUser) {
        AppUser user = userRepository.findById(authenticatedUser.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));

        List<Membership> memberships = membershipRepository.findByUserIdAndStatus(user.getId(), "ACTIVE");
        List<UUID> tenantIds = memberships.stream().map(Membership::getTenantId).toList();
        Map<UUID, String> orgNames = organizationRepository.findAllById(tenantIds).stream()
                .collect(Collectors.toMap(Organization::getId, Organization::getName));

        List<UserMembershipDto> membershipDtos = memberships.stream()
                .map(m -> new UserMembershipDto(
                        m.getTenantId(),
                        orgNames.getOrDefault(m.getTenantId(), "Organização"),
                        m.getRole(),
                        m.getStatus()
                ))
                .toList();

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                membershipDtos
        );
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
