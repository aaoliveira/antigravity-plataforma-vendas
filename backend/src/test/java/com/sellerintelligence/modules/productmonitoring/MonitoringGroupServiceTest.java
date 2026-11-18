package com.sellerintelligence.modules.productmonitoring;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.core.exception.DuplicateResourceException;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.productmonitoring.entity.MonitoringGroupRecord;
import com.sellerintelligence.modules.productmonitoring.model.CreateMonitoringGroupRequest;
import com.sellerintelligence.modules.productmonitoring.model.MonitoringGroupDto;
import com.sellerintelligence.modules.productmonitoring.model.MonitoringGroupStatus;
import com.sellerintelligence.modules.productmonitoring.model.UpdateMonitoringGroupRequest;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoredItemRepository;
import com.sellerintelligence.modules.productmonitoring.repository.MonitoringGroupRepository;
import com.sellerintelligence.modules.productmonitoring.service.MonitoringGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Collections;
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
class MonitoringGroupServiceTest {

    @Mock
    private MonitoringGroupRepository groupRepository;

    @Mock
    private MonitoredItemRepository itemRepository;

    @Mock
    private AuditPublisher auditPublisher;

    private MonitoringGroupService groupService;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        groupService = new MonitoringGroupService(groupRepository, itemRepository, auditPublisher);
    }

    @Test
    @DisplayName("Deve criar grupo de monitoramento com sucesso isolado por tenant")
    void shouldCreateGroupSuccessfully() {
        CreateMonitoringGroupRequest request = new CreateMonitoringGroupRequest(
                "Smartphones Concorrentes",
                "Monitoramento de preços Galaxy S23 e iPhone 13",
                "#10B981",
                60,
                90
        );

        when(groupRepository.existsByTenantIdAndNameIgnoreCaseAndStatusNot(
                eq(tenantId), eq("Smartphones Concorrentes"), eq(MonitoringGroupStatus.DELETED)
        )).thenReturn(false);

        when(groupRepository.save(any(MonitoringGroupRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MonitoringGroupDto dto = groupService.createGroup(tenantId, request);

        assertThat(dto).isNotNull();
        assertThat(dto.name()).isEqualTo("Smartphones Concorrentes");
        assertThat(dto.status()).isEqualTo(MonitoringGroupStatus.ACTIVE);
        assertThat(dto.minIntervalMinutes()).isEqualTo(60);
        assertThat(dto.retentionDays()).isEqualTo(90);

        verify(auditPublisher).publish(
                eq("MONITORING_GROUP_CREATED"),
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
    @DisplayName("Deve rejeitar criação de grupo com nome duplicado no mesmo tenant")
    void shouldRejectDuplicateGroupName() {
        CreateMonitoringGroupRequest request = new CreateMonitoringGroupRequest(
                "Eletrônicos",
                "Descrição",
                "#3B82F6",
                60,
                90
        );

        when(groupRepository.existsByTenantIdAndNameIgnoreCaseAndStatusNot(
                eq(tenantId), eq("Eletrônicos"), eq(MonitoringGroupStatus.DELETED)
        )).thenReturn(true);

        assertThatThrownBy(() -> groupService.createGroup(tenantId, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Já existe um grupo ativo ou pausado com o nome");
    }

    @Test
    @DisplayName("Deve recuperar grupo existente e contabilizar total de itens ativos")
    void shouldGetGroupWithItemCount() {
        UUID groupId = UUID.randomUUID();
        MonitoringGroupRecord record = new MonitoringGroupRecord(
                groupId, tenantId, "Hardware", "Grupo de PCs", "#6366F1", 30, 90,
                MonitoringGroupStatus.ACTIVE, null, Instant.now(), Instant.now()
        );

        when(groupRepository.findByIdAndTenantId(groupId, tenantId)).thenReturn(Optional.of(record));
        when(itemRepository.countByTenantIdAndGroupIdAndStatusNot(
                eq(tenantId), eq(groupId), eq(com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus.DELETED)
        )).thenReturn(5);

        MonitoringGroupDto dto = groupService.getGroup(tenantId, groupId);

        assertThat(dto.id()).isEqualTo(groupId);
        assertThat(dto.name()).isEqualTo("Hardware");
        assertThat(dto.totalItems()).isEqualTo(5);
    }

    @Test
    @DisplayName("Deve lançar 404 ao buscar grupo inexistente ou de outro tenant")
    void shouldThrowWhenGroupNotFound() {
        UUID groupId = UUID.randomUUID();
        when(groupRepository.findByIdAndTenantId(groupId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroup(tenantId, groupId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Grupo de monitoramento não encontrado");
    }

    @Test
    @DisplayName("Deve listar grupos com paginação e busca por nome")
    void shouldListGroupsWithSearch() {
        MonitoringGroupRecord record = new MonitoringGroupRecord(
                UUID.randomUUID(), tenantId, "Smartphones", null, "#3B82F6", 60, 90,
                MonitoringGroupStatus.ACTIVE, null, Instant.now(), Instant.now()
        );

        when(groupRepository.searchByTenantIdAndName(eq(tenantId), eq("smart"), any()))
                .thenReturn(new PageImpl<>(List.of(record)));

        Page<MonitoringGroupDto> result = groupService.listGroups(tenantId, "smart", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Smartphones");
    }

    @Test
    @DisplayName("Deve atualizar grupo e verificar duplicidade de nome")
    void shouldUpdateGroup() {
        UUID groupId = UUID.randomUUID();
        MonitoringGroupRecord record = new MonitoringGroupRecord(
                groupId, tenantId, "Nome Antigo", "Desc", "#3B82F6", 60, 90,
                MonitoringGroupStatus.ACTIVE, null, Instant.now(), Instant.now()
        );

        when(groupRepository.findByIdAndTenantId(groupId, tenantId)).thenReturn(Optional.of(record));
        when(groupRepository.existsByTenantIdAndNameIgnoreCaseAndStatusNot(
                eq(tenantId), eq("Nome Novo"), eq(MonitoringGroupStatus.DELETED)
        )).thenReturn(false);
        when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateMonitoringGroupRequest updateReq = new UpdateMonitoringGroupRequest(
                "Nome Novo", "Nova Desc", "#EF4444", 45, 120, MonitoringGroupStatus.PAUSED
        );

        MonitoringGroupDto updated = groupService.updateGroup(tenantId, groupId, updateReq);

        assertThat(updated.name()).isEqualTo("Nome Novo");
        assertThat(updated.description()).isEqualTo("Nova Desc");
        assertThat(updated.color()).isEqualTo("#EF4444");
        assertThat(updated.minIntervalMinutes()).isEqualTo(45);
        assertThat(updated.retentionDays()).isEqualTo(120);
        assertThat(updated.status()).isEqualTo(MonitoringGroupStatus.PAUSED);
    }

    @Test
    @DisplayName("Deve excluir grupo logicamente e marcar seus itens como DELETED")
    void shouldDeleteGroupSoftly() {
        UUID groupId = UUID.randomUUID();
        MonitoringGroupRecord record = new MonitoringGroupRecord(
                groupId, tenantId, "Grupo Descartado", null, "#3B82F6", 60, 90,
                MonitoringGroupStatus.ACTIVE, null, Instant.now(), Instant.now()
        );

        when(groupRepository.findByIdAndTenantId(groupId, tenantId)).thenReturn(Optional.of(record));
        when(itemRepository.findAllByTenantIdAndGroupIdAndStatusNot(
                eq(tenantId), eq(groupId), eq(com.sellerintelligence.modules.productmonitoring.model.MonitoredItemStatus.DELETED)
        )).thenReturn(Collections.emptyList());

        groupService.deleteGroup(tenantId, groupId);

        assertThat(record.getStatus()).isEqualTo(MonitoringGroupStatus.DELETED);
        assertThat(record.getDeletedAt()).isNotNull();
        verify(groupRepository).save(record);
    }
}
