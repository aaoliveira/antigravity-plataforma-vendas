package br.com.sellerintelligence.sync.csv;

import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.sync.domain.*;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.company.TaxRegime;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationRepository;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CsvImportServiceTest {

    @Autowired
    private CsvImportService csvImportService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private StoreRepository storeRepository;

    private UUID tenantId;
    private Store store;

    @BeforeEach
    void setUp() {
        Organization org = new Organization("CSV Test Org", "csv-test-org");
        org = organizationRepository.save(org);
        tenantId = org.getId();

        Company company = new Company(tenantId, "12345678000195", "Razao Teste", "Fantasia Teste", TaxRegime.SIMPLES_NACIONAL);
        company = companyRepository.save(company);

        store = new Store(tenantId, company, Marketplace.MERCADO_LIVRE, "Loja CSV Teste");
        store.setExternalAccountId("EXT-100");
        store = storeRepository.save(store);
    }

    @Test
    @DisplayName("Should successfully import valid catalog CSV with DataProvenance.IMPORTED")
    void shouldImportValidCatalogCsv() {
        String csvContent = """
                sku,title,brand,category,barcode,price,available_quantity,status
                SKU-001,Teclado Mecanico,GamerBrand,Informatica,7891234567890,199.90,25,ACTIVE
                SKU-002,Mouse Sem Fio,OfficeBrand,Perifericos,7899876543210,89.50,10,ACTIVE
                """;

        ByteArrayInputStream input = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        CsvValidationReport report = csvImportService.importCatalogCsv(tenantId, store.getId(), input);

        assertThat(report.totalRows()).isEqualTo(2);
        assertThat(report.importedRows()).isEqualTo(2);
        assertThat(report.rejectedRows()).isEqualTo(0);
        assertThat(report.errors()).isEmpty();

        Product product1 = productRepository.findByTenantIdAndStoreIdAndSku(tenantId, store.getId(), "SKU-001").orElseThrow();
        assertThat(product1.getTitle()).isEqualTo("Teclado Mecanico");
        assertThat(product1.getProvenance()).isEqualTo(DataProvenance.IMPORTED);

        Listing listing1 = listingRepository.findByTenantIdAndMarketplaceAndExternalListingId(
                tenantId, store.getMarketplace(), "CSV-SKU-001"
        ).orElseThrow();
        assertThat(listing1.getPrice()).isEqualByComparingTo(new BigDecimal("199.90"));
        assertThat(listing1.getAvailableQuantity()).isEqualTo(25);
        assertThat(listing1.getProvenance()).isEqualTo(DataProvenance.IMPORTED);
    }

    @Test
    @DisplayName("Should reject invalid catalog rows with line number and reason, without corrupting valid rows")
    void shouldRejectInvalidCatalogRowsWithDetailedErrors() {
        String csvContent = """
                sku,title,brand,category,barcode,price,available_quantity,status
                SKU-OK,Produto Valido,Marca,Cat,123,50.00,5,ACTIVE
                ,Produto Sem Sku,Marca,Cat,123,50.00,5,ACTIVE
                SKU-BAD-PRICE,Preco Invalido,Marca,Cat,123,NOT_A_NUMBER,5,ACTIVE
                SKU-NEG-QTY,Qtd Negativa,Marca,Cat,123,50.00,-10,ACTIVE
                """;

        ByteArrayInputStream input = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        CsvValidationReport report = csvImportService.importCatalogCsv(tenantId, store.getId(), input);

        assertThat(report.totalRows()).isEqualTo(4);
        assertThat(report.importedRows()).isEqualTo(1);
        assertThat(report.rejectedRows()).isEqualTo(3);
        assertThat(report.hasErrors()).isTrue();
        assertThat(report.errors()).hasSize(3);

        // Line 3: empty SKU
        assertThat(report.errors().get(0).lineNumber()).isEqualTo(3);
        assertThat(report.errors().get(0).field()).isEqualTo("sku");

        // Line 4: invalid price
        assertThat(report.errors().get(1).lineNumber()).isEqualTo(4);
        assertThat(report.errors().get(1).field()).isEqualTo("price");

        // Line 5: negative quantity
        assertThat(report.errors().get(2).lineNumber()).isEqualTo(5);
        assertThat(report.errors().get(2).field()).isEqualTo("available_quantity");

        // Verify valid row is in DB and invalid rows are NOT in DB
        assertThat(productRepository.findByTenantIdAndStoreIdAndSku(tenantId, store.getId(), "SKU-OK")).isPresent();
        assertThat(productRepository.findByTenantIdAndStoreIdAndSku(tenantId, store.getId(), "SKU-BAD-PRICE")).isEmpty();
    }

    @Test
    @DisplayName("Should reject catalog CSV missing mandatory header columns")
    void shouldRejectCatalogCsvWithMissingHeaders() {
        String invalidHeaderCsv = """
                brand,category,barcode
                Marca,Categoria,12345
                """;

        ByteArrayInputStream input = new ByteArrayInputStream(invalidHeaderCsv.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> csvImportService.importCatalogCsv(tenantId, store.getId(), input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must contain at least 'sku', 'title', and 'price'");
    }

    @Test
    @DisplayName("Should successfully import valid orders CSV with items")
    void shouldImportValidOrdersCsv() {
        String csvContent = """
                external_order_id,order_date,status,sku,item_title,quantity,unit_price,total_amount
                PED-2026-X1,2026-11-17T12:00:00Z,PAID,SKU-001,Teclado Mecanico,2,100.00,200.00
                PED-2026-X2,2026-11-17,PAID,SKU-002,Mouse Sem Fio,1,50.00,50.00
                """;

        ByteArrayInputStream input = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        CsvValidationReport report = csvImportService.importOrdersCsv(tenantId, store.getId(), store.getMarketplace(), input);

        assertThat(report.totalRows()).isEqualTo(2);
        assertThat(report.importedRows()).isEqualTo(2);
        assertThat(report.rejectedRows()).isEqualTo(0);

        Order order1 = orderRepository.findByTenantIdAndMarketplaceAndExternalOrderId(
                tenantId, store.getMarketplace(), "PED-2026-X1"
        ).orElseThrow();

        assertThat(order1.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(order1.getStatus()).isEqualTo("PAID");
        assertThat(order1.getProvenance()).isEqualTo(DataProvenance.IMPORTED);
        assertThat(order1.getItems()).hasSize(1);
        assertThat(order1.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should reject invalid orders rows with line number and reason")
    void shouldRejectInvalidOrdersRows() {
        String csvContent = """
                external_order_id,order_date,status,sku,item_title,quantity,unit_price,total_amount
                PED-OK,2026-11-17T12:00:00Z,PAID,SKU-001,Item OK,1,100.00,100.00
                ,2026-11-17T12:00:00Z,PAID,SKU-001,Item Sem ID,1,100.00,100.00
                PED-BAD-DATE,DATA_INVALIDA,PAID,SKU-001,Item Data Ruim,1,100.00,100.00
                PED-BAD-TOTAL,2026-11-17T12:00:00Z,PAID,SKU-001,Item Total Ruim,1,100.00,-50.00
                """;

        ByteArrayInputStream input = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        CsvValidationReport report = csvImportService.importOrdersCsv(tenantId, store.getId(), store.getMarketplace(), input);

        assertThat(report.totalRows()).isEqualTo(4);
        assertThat(report.importedRows()).isEqualTo(1);
        assertThat(report.rejectedRows()).isEqualTo(3);
        assertThat(report.errors()).hasSize(3);

        assertThat(report.errors().get(0).lineNumber()).isEqualTo(3);
        assertThat(report.errors().get(0).field()).isEqualTo("external_order_id");

        assertThat(report.errors().get(1).lineNumber()).isEqualTo(4);
        assertThat(report.errors().get(1).field()).isEqualTo("order_date");

        assertThat(report.errors().get(2).lineNumber()).isEqualTo(5);
        assertThat(report.errors().get(2).field()).isEqualTo("total_amount");

        assertThat(orderRepository.findByTenantIdAndMarketplaceAndExternalOrderId(tenantId, store.getMarketplace(), "PED-OK")).isPresent();
        assertThat(orderRepository.findByTenantIdAndMarketplaceAndExternalOrderId(tenantId, store.getMarketplace(), "PED-BAD-DATE")).isEmpty();
    }
}
