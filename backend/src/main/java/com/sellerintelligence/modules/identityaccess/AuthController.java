package com.sellerintelligence.modules.identityaccess;

import com.sellerintelligence.core.security.AuthenticatedUser;
import com.sellerintelligence.modules.identityaccess.dto.AuthResponse;
import com.sellerintelligence.modules.identityaccess.dto.LoginRequest;
import com.sellerintelligence.modules.identityaccess.dto.SwitchTenantRequest;
import com.sellerintelligence.modules.identityaccess.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de autenticação mínima, login, troca de tenant e perfil do usuário.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Autenticação mínima, emissão de JWT e resolução de contexto de tenant")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticação por e-mail e senha", description = "Valida credenciais, aplica proteção contra força bruta e retorna token JWT com tenant derivado")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/switch-tenant")
    @Operation(summary = "Alterna o tenant ativo do usuário", description = "Valida se o usuário autenticado possui membership ativa no tenant destino e emite novo token JWT")
    public ResponseEntity<AuthResponse> switchTenant(
            @Valid @RequestBody SwitchTenantRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.switchTenant(request.targetTenantId(), user, httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna os dados do perfil do usuário autenticado", description = "Exibe dados cadastrais e lista de organizações/memberships ativas do usuário")
    public ResponseEntity<UserProfileResponse> getMe(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        UserProfileResponse response = authService.getMe(user);
        return ResponseEntity.ok(response);
    }
}
