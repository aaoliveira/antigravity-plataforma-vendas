package br.com.sellerintelligence.web.controller;

import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserService;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreService;
import br.com.sellerintelligence.tenancy.store.StoreStatus;
import br.com.sellerintelligence.web.dto.CreateStoreRequest;
import br.com.sellerintelligence.web.dto.StoreResponse;
import br.com.sellerintelligence.web.dto.UpdateStoreRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;
    private final UserService userService;

    public StoreController(StoreService storeService, UserService userService) {
        this.storeService = storeService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<StoreResponse> createStore(
            @Valid @RequestBody CreateStoreRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        Store store = storeService.createStore(
                tenantId,
                request.companyId(),
                request.marketplace(),
                request.internalName(),
                request.metadata(),
                actor
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(StoreResponse.from(store));
    }

    @GetMapping
    public ResponseEntity<List<StoreResponse>> getStores(
            @RequestParam(required = false) UUID companyId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        List<Store> stores = storeService.getStoresForUser(tenantId, actor.getId(), companyId);
        List<StoreResponse> responses = stores.stream()
                .map(StoreResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreResponse> getStore(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        Store store = storeService.getStoreById(tenantId, id, actor.getId());
        return ResponseEntity.ok(StoreResponse.from(store));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreResponse> updateStore(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStoreRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        Store store = storeService.updateStore(
                tenantId,
                id,
                request.internalName(),
                request.externalAccountId(),
                request.connectionStatus(),
                request.metadata(),
                actor
        );

        return ResponseEntity.ok(StoreResponse.from(store));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<StoreResponse> setStoreStatus(
            @PathVariable UUID id,
            @RequestParam StoreStatus status,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        User actor = userService.resolveOrCreateUserFromJwt(jwt);

        Store store = storeService.setStoreStatus(tenantId, id, status, actor);
        return ResponseEntity.ok(StoreResponse.from(store));
    }
}
