package br.com.sellerintelligence.web.controller;

import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserService;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationService;
import br.com.sellerintelligence.tenancy.domain.Role;
import br.com.sellerintelligence.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserService userService;

    public OrganizationController(OrganizationService organizationService, UserService userService) {
        this.organizationService = organizationService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        User creator = userService.resolveOrCreateUserFromJwt(jwt);
        Organization org = organizationService.createOrganization(request.name(), request.slug(), creator);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrganizationResponse.from(org, Role.OWNER));
    }

    @GetMapping("/me")
    public ResponseEntity<List<OrganizationResponse>> getMyOrganizations(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.resolveOrCreateUserFromJwt(jwt);
        List<Membership> memberships = organizationService.getUserMemberships(user.getId());
        List<OrganizationResponse> responses = memberships.stream()
                .map(m -> OrganizationResponse.from(m.getOrganization(), m.getRole()))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganization(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.resolveOrCreateUserFromJwt(jwt);
        Organization org = organizationService.getOrganizationForUser(id, user.getId());
        Membership membership = organizationService.findActiveMembership(id, jwt.getSubject())
                .orElseThrow();
        return ResponseEntity.ok(OrganizationResponse.from(org, membership.getRole()));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<MemberResponse>> listMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.resolveOrCreateUserFromJwt(jwt);
        List<Membership> members = organizationService.getOrganizationMembers(id, user.getId());
        List<MemberResponse> responses = members.stream()
                .map(MemberResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        User actor = userService.resolveOrCreateUserFromJwt(jwt);
        Membership membership = organizationService.addOrInviteMember(
                id,
                request.email(),
                request.name(),
                request.role(),
                actor
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(membership));
    }

    @PatchMapping("/{id}/members/{userId}/role")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        User actor = userService.resolveOrCreateUserFromJwt(jwt);
        Membership updated = organizationService.updateMemberRole(id, userId, request.role(), actor);
        return ResponseEntity.ok(MemberResponse.from(updated));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        User actor = userService.resolveOrCreateUserFromJwt(jwt);
        organizationService.removeMember(id, userId, actor);
        return ResponseEntity.noContent().build();
    }
}
