package com.sellerintelligence.modules.pricing;

import com.sellerintelligence.core.security.AuthenticatedUser;
import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.pricing.model.FeeResolutionRequest;
import com.sellerintelligence.modules.pricing.model.FeeResolutionResult;
import com.sellerintelligence.modules.pricing.model.FeeRuleDto;
import com.sellerintelligence.modules.pricing.model.ProductCostDto;
import com.sellerintelligence.modules.pricing.model.RegisterFeeRuleCommand;
import com.sellerintelligence.modules.pricing.model.RegisterProductCostCommand;
import com.sellerintelligence.modules.pricing.service.FeeResolutionService;
import com.sellerintelligence.modules.pricing.service.FeeRuleService;
import com.sellerintelligence.modules.pricing.service.ProductCostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller REST para cadastro de custos por produto/variante/empresa e gestão de regras de comissões e taxas.
 */
@RestController
@RequestMapping("/api/v1/pricing")
public class PricingAndCostsController {

    private final ProductCostService productCostService;
    private final FeeRuleService feeRuleService;
    private final FeeResolutionService feeResolutionService;

    public PricingAndCostsController(
            ProductCostService productCostService,
            FeeRuleService feeRuleService,
            FeeResolutionService feeResolutionService
    ) {
        this.productCostService = productCostService;
        this.feeRuleService = feeRuleService;
        this.feeResolutionService = feeResolutionService;
    }

    // --- CUSTOS DE PRODUTOS ---

    @PostMapping("/costs")
    public ResponseEntity<ProductCostDto> registerProductCost(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody RegisterProductCostCommand command
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String userEmail = (user != null) ? user.getEmail() : "user@tenant.local";

        ProductCostDto created = productCostService.registerCost(tenantId, userEmail, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/costs/effective")
    public ResponseEntity<ProductCostDto> getEffectiveProductCost(
            @RequestParam UUID productId,
            @RequestParam(required = false) UUID variantId,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) Instant referenceDate
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        Optional<ProductCostDto> costOpt = productCostService.getEffectiveCost(
                tenantId, productId, variantId, companyId, referenceDate
        );

        return costOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/costs/history")
    public ResponseEntity<List<ProductCostDto>> getProductCostHistory(
            @RequestParam UUID productId
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        List<ProductCostDto> history = productCostService.listCostHistory(tenantId, productId);
        return ResponseEntity.ok(history);
    }

    // --- REGRAS DE TAXAS E COMISSÕES ---

    @PostMapping("/fee-rules")
    public ResponseEntity<FeeRuleDto> registerFeeRule(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody RegisterFeeRuleCommand command
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        String userEmail = (user != null) ? user.getEmail() : "user@tenant.local";

        FeeRuleDto created = feeRuleService.registerRule(tenantId, userEmail, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/fee-rules")
    public ResponseEntity<List<FeeRuleDto>> listFeeRules(
            @RequestParam(required = false) String marketplace
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        List<FeeRuleDto> rules = feeRuleService.listRules(tenantId, marketplace);
        return ResponseEntity.ok(rules);
    }

    @PostMapping("/fee-rules/resolve")
    public ResponseEntity<FeeResolutionResult> resolveFees(
            @Valid @RequestBody FeeResolutionRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        FeeResolutionResult result = feeResolutionService.resolveFees(tenantId, request);
        return ResponseEntity.ok(result);
    }
}
