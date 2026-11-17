package br.com.sellerintelligence.sync.csv;

import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.sync.domain.*;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);

    private final ProductRepository productRepository;
    private final ListingRepository listingRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;

    public CsvImportService(
            ProductRepository productRepository,
            ListingRepository listingRepository,
            OrderRepository orderRepository,
            StoreRepository storeRepository) {
        this.productRepository = productRepository;
        this.listingRepository = listingRepository;
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
    }

    public String getCatalogTemplate() {
        return "sku,title,brand,category,barcode,price,available_quantity,status\n" +
                "CAMISA-AZUL-G,Camisa Polo Azul G,MarcaX,Vestuario,7891234567890,89.90,15,ACTIVE\n" +
                "TENIS-RUN-41,Tenis de Corrida 41,MarcaY,Calcados,7899876543210,249.90,8,ACTIVE\n";
    }

    public String getOrdersTemplate() {
        return "external_order_id,order_date,status,sku,item_title,quantity,unit_price,total_amount\n" +
                "PED-2026-001,2026-11-17T10:00:00Z,PAID,CAMISA-AZUL-G,Camisa Polo Azul G,1,89.90,89.90\n" +
                "PED-2026-002,2026-11-17T11:30:00Z,PAID,TENIS-RUN-41,Tenis de Corrida 41,1,249.90,249.90\n";
    }

    @Transactional
    public CsvValidationReport importCatalogCsv(UUID tenantId, UUID storeId, InputStream inputStream) {
        Store store = storeRepository.findByIdAndTenantId(storeId, tenantId)
                .orElseThrow(() -> new BusinessException("STORE_NOT_FOUND", "Store not found or access denied", HttpStatus.NOT_FOUND));

        List<CsvRowError> errors = new ArrayList<>();
        int totalRows = 0;
        int importedRows = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = new CSVParser(new InputStreamReader(inputStream, StandardCharsets.UTF_8), format)) {
            Map<String, Integer> headerMap = parser.getHeaderMap();
            if (headerMap == null || !headerMap.containsKey("sku") || !headerMap.containsKey("title") || !headerMap.containsKey("price")) {
                throw new BusinessException(
                        "INVALID_CSV_HEADER",
                        "Catalog CSV must contain at least 'sku', 'title', and 'price' columns",
                        HttpStatus.BAD_REQUEST
                );
            }

            for (CSVRecord record : parser) {
                totalRows++;
                int lineNumber = (int) record.getRecordNumber() + 1; // 1-based, accounting for header

                String sku = record.isSet("sku") ? record.get("sku") : null;
                String title = record.isSet("title") ? record.get("title") : null;
                String brand = record.isSet("brand") ? record.get("brand") : null;
                String category = record.isSet("category") ? record.get("category") : null;
                String barcode = record.isSet("barcode") ? record.get("barcode") : null;
                String priceStr = record.isSet("price") ? record.get("price") : null;
                String qtyStr = record.isSet("available_quantity") ? record.get("available_quantity") : null;
                String status = record.isSet("status") ? record.get("status") : "ACTIVE";

                // Validations
                if (sku == null || sku.isBlank()) {
                    errors.add(new CsvRowError(lineNumber, "sku", sku, "SKU cannot be empty"));
                    continue;
                }
                if (title == null || title.isBlank()) {
                    errors.add(new CsvRowError(lineNumber, "title", title, "Title cannot be empty"));
                    continue;
                }

                BigDecimal price;
                try {
                    if (priceStr == null || priceStr.isBlank()) {
                        errors.add(new CsvRowError(lineNumber, "price", priceStr, "Price cannot be empty"));
                        continue;
                    }
                    price = new BigDecimal(priceStr.replace(',', '.'));
                    if (price.compareTo(BigDecimal.ZERO) < 0) {
                        errors.add(new CsvRowError(lineNumber, "price", priceStr, "Price must be non-negative"));
                        continue;
                    }
                } catch (NumberFormatException e) {
                    errors.add(new CsvRowError(lineNumber, "price", priceStr, "Invalid numeric format for price"));
                    continue;
                }

                Integer availableQty = null;
                if (qtyStr != null && !qtyStr.isBlank()) {
                    try {
                        availableQty = Integer.parseInt(qtyStr);
                        if (availableQty < 0) {
                            errors.add(new CsvRowError(lineNumber, "available_quantity", qtyStr, "Quantity must be non-negative"));
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        errors.add(new CsvRowError(lineNumber, "available_quantity", qtyStr, "Invalid integer format for quantity"));
                        continue;
                    }
                }

                // Upsert Product
                Product product = productRepository.findByTenantIdAndStoreIdAndSku(tenantId, storeId, sku)
                        .orElse(null);

                if (product == null) {
                    product = new Product(
                            tenantId, storeId, sku, title, brand, category, barcode, null, DataProvenance.IMPORTED
                    );
                } else {
                    product.setTitle(title);
                    if (brand != null && !brand.isBlank()) product.setBrand(brand);
                    if (category != null && !category.isBlank()) product.setCategory(category);
                    if (barcode != null && !barcode.isBlank()) product.setBarcode(barcode);
                }
                product = productRepository.save(product);

                // Upsert Listing associated with store's marketplace
                String externalListingId = "CSV-" + sku;
                Listing listing = listingRepository.findByTenantIdAndMarketplaceAndExternalListingId(
                        tenantId, store.getMarketplace(), externalListingId
                ).orElse(null);

                if (listing == null) {
                    listing = new Listing(
                            tenantId, storeId, product.getId(), store.getMarketplace(),
                            externalListingId, sku, title, status != null && !status.isBlank() ? status : "ACTIVE",
                            price, "BRL", availableQty, 0,
                            "CSV_IMPORT", null, null,
                            DataProvenance.IMPORTED, null, Instant.now()
                    );
                } else {
                    listing.update(title, status, price, availableQty, listing.getSoldQuantity(),
                            listing.getListingType(), listing.getPermalink(), listing.getThumbnailUrl(),
                            listing.getRawPayload(), Instant.now());
                }
                listingRepository.save(listing);
                importedRows++;
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Failed to parse catalog CSV", e);
            throw new BusinessException("CSV_PARSE_ERROR", "Failed to parse CSV file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new CsvValidationReport("CATALOG", totalRows, importedRows, errors.size(), errors);
    }

    @Transactional
    public CsvValidationReport importOrdersCsv(UUID tenantId, UUID storeId, Marketplace marketplace, InputStream inputStream) {
        storeRepository.findByIdAndTenantId(storeId, tenantId)
                .orElseThrow(() -> new BusinessException("STORE_NOT_FOUND", "Store not found or access denied", HttpStatus.NOT_FOUND));

        List<CsvRowError> errors = new ArrayList<>();
        int totalRows = 0;
        int importedRows = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = new CSVParser(new InputStreamReader(inputStream, StandardCharsets.UTF_8), format)) {
            Map<String, Integer> headerMap = parser.getHeaderMap();
            if (headerMap == null || !headerMap.containsKey("external_order_id") || !headerMap.containsKey("order_date")
                    || !headerMap.containsKey("status") || !headerMap.containsKey("total_amount")) {
                throw new BusinessException(
                        "INVALID_CSV_HEADER",
                        "Orders CSV must contain at least 'external_order_id', 'order_date', 'status', and 'total_amount' columns",
                        HttpStatus.BAD_REQUEST
                );
            }

            for (CSVRecord record : parser) {
                totalRows++;
                int lineNumber = (int) record.getRecordNumber() + 1;

                String externalOrderId = record.isSet("external_order_id") ? record.get("external_order_id") : null;
                String orderDateStr = record.isSet("order_date") ? record.get("order_date") : null;
                String status = record.isSet("status") ? record.get("status") : null;
                String totalAmountStr = record.isSet("total_amount") ? record.get("total_amount") : null;
                String sku = record.isSet("sku") ? record.get("sku") : null;
                String itemTitle = record.isSet("item_title") ? record.get("item_title") : "Item Importado";
                String qtyStr = record.isSet("quantity") ? record.get("quantity") : "1";
                String unitPriceStr = record.isSet("unit_price") ? record.get("unit_price") : null;

                if (externalOrderId == null || externalOrderId.isBlank()) {
                    errors.add(new CsvRowError(lineNumber, "external_order_id", externalOrderId, "External order ID cannot be empty"));
                    continue;
                }
                if (status == null || status.isBlank()) {
                    errors.add(new CsvRowError(lineNumber, "status", status, "Status cannot be empty"));
                    continue;
                }

                Instant orderDate;
                try {
                    if (orderDateStr == null || orderDateStr.isBlank()) {
                        errors.add(new CsvRowError(lineNumber, "order_date", orderDateStr, "Order date cannot be empty"));
                        continue;
                    }
                    if (orderDateStr.length() == 10) {
                        orderDate = LocalDate.parse(orderDateStr).atStartOfDay().toInstant(ZoneOffset.UTC);
                    } else {
                        orderDate = Instant.parse(orderDateStr);
                    }
                } catch (DateTimeParseException e) {
                    errors.add(new CsvRowError(lineNumber, "order_date", orderDateStr, "Invalid date format. Use ISO-8601 (e.g., 2026-11-17T10:00:00Z or YYYY-MM-DD)"));
                    continue;
                }

                BigDecimal totalAmount;
                try {
                    if (totalAmountStr == null || totalAmountStr.isBlank()) {
                        errors.add(new CsvRowError(lineNumber, "total_amount", totalAmountStr, "Total amount cannot be empty"));
                        continue;
                    }
                    totalAmount = new BigDecimal(totalAmountStr.replace(',', '.'));
                    if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                        errors.add(new CsvRowError(lineNumber, "total_amount", totalAmountStr, "Total amount must be non-negative"));
                        continue;
                    }
                } catch (NumberFormatException e) {
                    errors.add(new CsvRowError(lineNumber, "total_amount", totalAmountStr, "Invalid numeric format for total amount"));
                    continue;
                }

                int quantity;
                try {
                    quantity = Integer.parseInt(qtyStr);
                    if (quantity <= 0) {
                        errors.add(new CsvRowError(lineNumber, "quantity", qtyStr, "Quantity must be greater than zero"));
                        continue;
                    }
                } catch (NumberFormatException e) {
                    errors.add(new CsvRowError(lineNumber, "quantity", qtyStr, "Invalid integer format for quantity"));
                    continue;
                }

                BigDecimal unitPrice;
                try {
                    if (unitPriceStr == null || unitPriceStr.isBlank()) {
                        unitPrice = totalAmount.divide(BigDecimal.valueOf(quantity), 2, java.math.RoundingMode.HALF_UP);
                    } else {
                        unitPrice = new BigDecimal(unitPriceStr.replace(',', '.'));
                    }
                } catch (Exception e) {
                    errors.add(new CsvRowError(lineNumber, "unit_price", unitPriceStr, "Invalid numeric format for unit price"));
                    continue;
                }

                // Upsert Order
                Order order = orderRepository.findByTenantIdAndMarketplaceAndExternalOrderId(
                        tenantId, marketplace, externalOrderId
                ).orElse(null);

                if (order == null) {
                    order = new Order(
                            tenantId, storeId, marketplace, externalOrderId, status,
                            totalAmount, "BRL", orderDate, "comprador_csv",
                            null, null, null,
                            DataProvenance.IMPORTED, null, Instant.now()
                    );
                    OrderItem item = new OrderItem(
                            tenantId, order, null, sku, itemTitle, unitPrice, quantity, null
                    );
                    order.addItem(item);
                } else {
                    order.update(status, totalAmount, order.getShippingCostSeller(), order.getMarketplaceFee(), null, Instant.now());
                }
                orderRepository.save(order);
                importedRows++;
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Failed to parse orders CSV", e);
            throw new BusinessException("CSV_PARSE_ERROR", "Failed to parse orders CSV: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new CsvValidationReport("ORDERS", totalRows, importedRows, errors.size(), errors);
    }
}
