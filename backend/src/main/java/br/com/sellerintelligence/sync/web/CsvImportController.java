package br.com.sellerintelligence.sync.web;

import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.sync.csv.CsvImportService;
import br.com.sellerintelligence.sync.csv.CsvValidationReport;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync/csv")
public class CsvImportController {

    private final CsvImportService csvImportService;

    public CsvImportController(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    @PostMapping(value = "/catalog", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CsvValidationReport> importCatalog(
            @RequestParam("storeId") UUID storeId,
            @RequestParam("file") MultipartFile file) throws IOException {
        UUID tenantId = TenantContext.getTenantId();
        CsvValidationReport report = csvImportService.importCatalogCsv(tenantId, storeId, file.getInputStream());
        return ResponseEntity.ok(report);
    }

    @PostMapping(value = "/orders", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CsvValidationReport> importOrders(
            @RequestParam("storeId") UUID storeId,
            @RequestParam(value = "marketplace", defaultValue = "OUTRO") Marketplace marketplace,
            @RequestParam("file") MultipartFile file) throws IOException {
        UUID tenantId = TenantContext.getTenantId();
        CsvValidationReport report = csvImportService.importOrdersCsv(tenantId, storeId, marketplace, file.getInputStream());
        return ResponseEntity.ok(report);
    }

    @GetMapping("/template/{type}")
    public ResponseEntity<String> getTemplate(@PathVariable String type) {
        String template;
        String filename;
        if ("orders".equalsIgnoreCase(type)) {
            template = csvImportService.getOrdersTemplate();
            filename = "template_pedidos.csv";
        } else {
            template = csvImportService.getCatalogTemplate();
            filename = "template_catalogo.csv";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(template);
    }
}
