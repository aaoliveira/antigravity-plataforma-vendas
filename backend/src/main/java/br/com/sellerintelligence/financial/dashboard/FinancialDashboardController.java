package br.com.sellerintelligence.financial.dashboard;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/financial/dashboard")
public class FinancialDashboardController {

    private final FinancialDashboardService dashboardService;

    public FinancialDashboardController(FinancialDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverview(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) Marketplace marketplace,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "true") boolean compareWithPrevious
    ) {
        FinancialDashboardFilter filter = new FinancialDashboardFilter(companyId, storeId, marketplace, from, to, compareWithPrevious);
        DashboardOverviewResponse response = dashboardService.getDashboardOverview(filter);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<FinancialOrderListItemDto>> getOrderList(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) Marketplace marketplace,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        FinancialDashboardFilter filter = new FinancialDashboardFilter(companyId, storeId, marketplace, from, to, false);
        List<FinancialOrderListItemDto> orders = dashboardService.getOrderList(filter);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) Marketplace marketplace,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "America/Sao_Paulo") String timezone
    ) {
        FinancialDashboardFilter filter = new FinancialDashboardFilter(companyId, storeId, marketplace, from, to, false);
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timezone);
        } catch (Exception e) {
            zoneId = ZoneId.of("America/Sao_Paulo");
        }

        String csvContent = dashboardService.exportOrdersCsv(filter, zoneId);
        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

        String filename = "relatorio_financeiro_" + System.currentTimeMillis() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }
}
