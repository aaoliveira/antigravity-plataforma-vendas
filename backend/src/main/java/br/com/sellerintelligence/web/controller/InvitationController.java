package br.com.sellerintelligence.web.controller;

import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserService;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.tenancy.invitation.InvitationService;
import br.com.sellerintelligence.tenancy.invitation.UserInvitation;
import br.com.sellerintelligence.web.dto.AcceptInvitationRequest;
import br.com.sellerintelligence.web.dto.CreateInvitationRequest;
import br.com.sellerintelligence.web.dto.InvitationResponse;
import br.com.sellerintelligence.web.dto.MemberResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final InvitationService invitationService;
    private final UserService userService;

    public InvitationController(InvitationService invitationService, UserService userService) {
        this.invitationService = invitationService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<InvitationResponse> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        UserInvitation invitation = invitationService.createInvitation(
                tenantId,
                request.email(),
                request.role(),
                request.allowedCompanyIds(),
                request.allowedStoreIds(),
                actor
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(InvitationResponse.from(invitation));
    }

    @GetMapping
    public ResponseEntity<List<InvitationResponse>> getPendingInvitations(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        List<UserInvitation> invitations = invitationService.getPendingInvitations(tenantId, actor.getId());
        List<InvitationResponse> responses = invitations.stream()
                .map(InvitationResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/accept")
    public ResponseEntity<MemberResponse> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        User acceptingUser = userService.resolveOrCreateUserFromJwt(jwt);
        Membership membership = invitationService.acceptInvitation(request.token(), acceptingUser);
        return ResponseEntity.ok(MemberResponse.from(membership));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeInvitation(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        invitationService.revokeInvitation(tenantId, id, actor);
        return ResponseEntity.noContent().build();
    }
}
