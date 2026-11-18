package com.sellerintelligence.modules.productmonitoring;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.BusinessRuleException;
import com.sellerintelligence.core.exception.DuplicateResourceException;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoredItemRecord;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoringGroupRecord;
import com.sellerintelligence.modules.productmonitoring.model.AddMonitoredItemRequest;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemDto;
import com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus;
import com.sellerintelligence.modules.productmonitoring.model.MonitoringGroupStatus;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemRepository;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemSnapshotRepository;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoringGroupRepository;
import com.sellerintelligence.modules.productmonitoring.service.MonitoredItemService;
import com.sellerintelligence.modules.productmonitoring.service.MonitoringSchedulerPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoredItemServiceTest {

    @Mock
    private MonitoredItemRepository itemRepository;

    @Mock
    private MonitoringGroupRepository groupRepository;

    @Mock
    private MonitoredItemSnapshotRepository snapshotRepository;

    @Mock
    private MonitoringSchedulerPolicyValidator policyValidator;

    @Mock
    private AuditPublisher auditPublisher;

    private MonitoredItemService itemService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        itemService = new MonitoredItemService(
                itemRepository, groupRepository, snapshotRepository, policyValidator, auditPublisher
        );
    }

    @Test
    @DisplayName("Deve adicionar item ao grupo com sucesso e inicializar firstSnapshotAt como nulo")
    void shouldAddItemSuccessfully() {
        MonitoringGroupRecord group = new MonitoringGroupRecord(
                groupId, tenantId, "Smartphones", null, "#3B82F6", 60, 90,
                MonitoringGroupStatus.ACTIVE, null, Instant.now(), Instant.now()
        );
        when(groupRepository.findByIdAndTenantId(groupId, tenantId)).thenReturn(Optional.of(group));
        when(policyValidator.isForbiddenSource("POLICY_MELI_OFFICIAL")).thenReturn(false);
        when(itemRepository.existsByTenantIdAndGroupIdAndMarketplaceAndExternalListingIdAndStatusNot(
                eq(tenantId), eq(groupId), eq("MERCADO_LIVRE"), eq("MLB-123456"), eq(MonitoredItemStatus.DELETED)
        )).thenReturn(false);
        when(itemRepository.save(any(MonitoredItemRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        AddMonitoredItemRequest request = new AddMonitoredItemRequest(
                "MERCADO_LIVRE",
                "POLICY_MELI_OFFICIAL",
                "MLB-123456",
                "Smartphone Galaxy S23 128GB Preto",
                new BigDecimal("3299.00"),
                "BRL",
                "https://mercadolivre.com.br/item/123",
                "Loja Autorizada",
                "NEW",
                60
        );

        MonitoredItemDto dto = itemService.addItemToGroup(tenantId, groupId, request);

        assertThat(dto).isNotNull();
        assertThat(dto.externalListingId()).isEqualTo("MLB-123456");
        assertThat(dto.marketplace()).isEqualTo("MERCADO_LIVRE");
        assertThat(dto.initialPrice()).isEqualByComparingTo("3299.00");
        assertThat(dto.status()).isEqualTo(MonitoredItemStatus.ACTIVE);
        assertThat(dto.firstSnapshotAt()).isNull(); // Histórico só começa no primeiro snapshot após inclusão!

        verify(auditPublisher).publish(
                eq("MONITORED_ITEM_ADDED"),
                any(),
                eq("system"),
                eq(tenantId),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Deve rejeitar inclusão de item duplicado no mesmo grupo (duplicidade controlada)")
    void shouldRejectDuplicateItemInSameGroup() {
        MonitoringGroupRecord group = new MonitoringGroupRecord(
                groupId, tenantId, "Smartphones", null, "#3B82F6", 60, 90,
                MonitoringGroupStatus.ACTIVE, null, Instant.now(), Instant.now()
        );
        when(groupRepository.findByIdAndTenantId(groupId, tenantId)).thenReturn(Optional.of(group));
        when(policyValidator.isForbiddenSource("POLICY_MELI_OFFICIAL")).thenReturn(false);
        when(itemRepository.existsByTenantIdAndGroupIdAndMarketplaceAndExternalListingIdAndStatusNot(
                eq(tenantId), eq(groupId), eq("MERCADO_LIVRE"), eq("MLB-123456"), eq(MonitoredItemStatus.DELETED)
        )).thenReturn(true);

        AddMonitoredItemRequest request = new AddMonitoredItemRequest(
                "MERCADO_LIVRE", "POLICY_MELI_OFFICIAL", "MLB-123456", "Título",
                new BigDecimal("100.00"), "BRL", null, null, "NEW", 60
        );

        assertThatThrownBy(() -> itemService.addItemToGroup(tenantId, groupId, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("já está adicionado ao grupo");
    }

    @Test
    @DisplayName("Deve rejeitar inclusão de item com fonte proibida")
    void shouldRejectItemWithForbiddenSource() {
        MonitoringGroupRecord group = new MonitoringGroupRecord(
                groupId, tenantId, "Smartphones", null, "#3B82F6", 60, 90,
                MonitoringGroupStatus.ACTIVE, null, Instant.now(), Instant.now()
        );
        when(groupRepository.findByIdAndTenantId(groupId, tenantId)).thenReturn(Optional.of(group));
        when(policyValidator.isForbiddenSource("UNLICENSED_SCRAPER")).thenReturn(true);

        AddMonitoredItemRequest request = new AddMonitoredItemRequest(
                "MERCADO_LIVRE", "UNLICENSED_SCRAPER", "MLB-999", "Título",
                new BigDecimal("100.00"), "BRL", null, null, "NEW", 60
        );

        assertThatThrownBy(() -> itemService.addItemToGroup(tenantId, groupId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("é proibida ou não homologada");
    }

    @Test
    @DisplayName("Deve remover item do grupo marcando como DELETED e auditando")
    void shouldRemoveItemFromGroup() {
        UUID itemId = UUID.randomUUID();
        MonitoredItemRecord item = new MonitoredItemRecord(
                itemId, tenantId, groupId, "MERCADO_LIVRE", "POL-1", "MLB-101",
                "Smartphone", new BigDecimal("3000.00"), new BigDecimal("3000.00"), "BRL",
                null, null, "NEW", MonitoredItemStatus.ACTIVE, 60, null, Instant.now(),
                null, null, Instant.now(), Instant.now()
        );

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));

        itemService.removeItemFromGroup(tenantId, groupId, itemId);

        assertThat(item.getStatus()).isEqualTo(MonitoredItemStatus.DELETED);
        assertThat(item.getDeletedAt()).isNotNull();
        verify(itemRepository).save(item);
    }

    @Test
    @DisplayName("Deve listar itens de um grupo com contagem de snapshots")
    void shouldListItemsInGroup() {
        MonitoringGroupRecord group = new MonitoringGroupRecord(
                groupId, tenantId, "Smartphones", null, "#3B82F6", 60, 90,
                MonitoringGroupStatus.ACTIVE, null, Instant.now(), Instant.now()
        );
        when(groupRepository.findByIdAndTenantId(groupId, tenantId)).thenReturn(Optional.of(group));

        UUID itemId = UUID.randomUUID();
        MonitoredItemRecord item = new MonitoredItemRecord(
                itemId, tenantId, groupId, "MERCADO_LIVRE", "POL-1", "MLB-101",
                "Smartphone", new BigDecimal("3000.00"), new BigDecimal("3000.00"), "BRL",
                null, null, "NEW", MonitoredItemStatus.ACTIVE, 60, null, Instant.now(),
                null, null, Instant.now(), Instant.now()
        );

        when(itemRepository.findAllByTenantIdAndGroupIdAndStatusNot(
                eq(tenantId), eq(groupId), eq(MonitoredItemStatus.DELETED), any()
        )).thenReturn(new PageImpl<>(List.of(item)));
        when(snapshotRepository.countByTenantIdAndItemId(tenantId, itemId)).thenReturn(3);

        Page<MonitoredItemDto> page = itemService.listItemsInGroup(tenantId, groupId, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).totalSnapshots()).isEqualTo(3);
    }
}
