package com.automotive.costing.versioning.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.versioning.dto.VersionControlDtos.CreateSnapshotRequest;
import com.automotive.costing.versioning.dto.VersionControlDtos.VersionResponse;
import com.automotive.costing.versioning.entity.ChangeTrigger;
import com.automotive.costing.versioning.entity.CostVersionRecord;
import com.automotive.costing.versioning.repository.CostVersionRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostVersionServiceTest {

    @Mock
    private CostVersionRecordRepository repository;
    @Mock
    private AuditService auditService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CostVersionService service;

    @BeforeEach
    void stubSave() {
        when(repository.save(any(CostVersionRecord.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createSnapshot_firstVersionIsOne() {
        when(repository.findFirstByEntityTypeAndEntityIdOrderByVersionNumberDesc("ShouldCostBuildUp", "e1"))
                .thenReturn(Optional.empty());

        VersionResponse resp = service.createSnapshot(new CreateSnapshotRequest(
                "ShouldCostBuildUp", "e1", ChangeTrigger.MANUAL_REVISION, null, "initial",
                Map.of("total", 10)));

        assertThat(resp.versionNumber()).isEqualTo(1);
        assertThat(resp.changeTrigger()).isEqualTo(ChangeTrigger.MANUAL_REVISION);
        ArgumentCaptor<CostVersionRecord> cap = ArgumentCaptor.forClass(CostVersionRecord.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getSnapshotJson()).contains("total");
    }

    @Test
    void createSnapshot_incrementsVersion() {
        CostVersionRecord existing = new CostVersionRecord();
        existing.setVersionNumber(3);
        when(repository.findFirstByEntityTypeAndEntityIdOrderByVersionNumberDesc("SupplierQuote", "q1"))
                .thenReturn(Optional.of(existing));

        VersionResponse resp = service.createSnapshot(new CreateSnapshotRequest(
                "SupplierQuote", "q1", ChangeTrigger.RFQ_ROUND, "RFQ-9", null, Map.of()));

        assertThat(resp.versionNumber()).isEqualTo(4);
    }

    @Test
    void designateBaseline_clearsPrior() {
        CostVersionRecord target = new CostVersionRecord();
        target.setVersionId("v1");
        target.setEntityType("TargetCostRecord");
        target.setEntityId("t1");
        target.setVersionNumber(2);
        target.setChangeTrigger(ChangeTrigger.APPROVAL_BASELINE);
        target.setSnapshotJson("{\"x\":1}");
        when(repository.findById("v1")).thenReturn(Optional.of(target));

        VersionResponse resp = service.designateBaseline("v1");
        verify(repository).clearBaseline("TargetCostRecord", "t1");
        assertThat(resp.programBaseline()).isTrue();
    }

    @Test
    void rollback_createsNewVersionWithTrigger() {
        CostVersionRecord prior = new CostVersionRecord();
        prior.setVersionId("v2");
        prior.setEntityType("ShouldCostBuildUp");
        prior.setEntityId("sc1");
        prior.setVersionNumber(2);
        prior.setChangeTrigger(ChangeTrigger.ECN);
        prior.setSnapshotJson("{\"cost\":100}");
        when(repository.findById("v2")).thenReturn(Optional.of(prior));
        when(repository.findFirstByEntityTypeAndEntityIdOrderByVersionNumberDesc("ShouldCostBuildUp", "sc1"))
                .thenReturn(Optional.of(prior));

        VersionResponse resp = service.rollback("v2", "restore");
        assertThat(resp.versionNumber()).isEqualTo(3);
        assertThat(resp.changeTrigger()).isEqualTo(ChangeTrigger.ROLLBACK);
    }

    @Test
    void snapshotAfterChange_bomRelease_usesEcnTrigger() {
        when(repository.findFirstByEntityTypeAndEntityIdOrderByVersionNumberDesc("BomNode", "bom-1"))
                .thenReturn(Optional.empty());

        VersionResponse resp = service.snapshotAfterChange(
                CostVersionService.TYPE_BOM_NODE,
                "bom-1",
                ChangeTrigger.ECN,
                "ECN-100",
                "BOM node released",
                Map.of("status", "RELEASED", "ecnReference", "ECN-100"));

        assertThat(resp.versionNumber()).isEqualTo(1);
        assertThat(resp.changeTrigger()).isEqualTo(ChangeTrigger.ECN);
        assertThat(resp.entityType()).isEqualTo("BomNode");
        ArgumentCaptor<CostVersionRecord> cap = ArgumentCaptor.forClass(CostVersionRecord.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getChangeReference()).isEqualTo("ECN-100");
        assertThat(cap.getValue().getSnapshotJson()).contains("RELEASED");
    }

}
