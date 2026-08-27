package com.automotive.costing.quantitystructure;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.costingvariant.entity.CostingVariant;
import com.automotive.costing.costingvariant.entity.CostingVariantStatus;
import com.automotive.costing.costingvariant.entity.QuantityStructureSource;
import com.automotive.costing.costingvariant.service.CostingVariantService;
import com.automotive.costing.masterdata.entity.LaborMaster;
import com.automotive.costing.masterdata.entity.MachineMaster;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.entity.MaterialMaster;
import com.automotive.costing.masterdata.repository.LaborMasterRepository;
import com.automotive.costing.masterdata.repository.MachineMasterRepository;
import com.automotive.costing.masterdata.repository.MaterialMasterRepository;
import com.automotive.costing.process.entity.ProcessOperation;
import com.automotive.costing.process.entity.ProcessRouting;
import com.automotive.costing.process.entity.RoutingStatus;
import com.automotive.costing.process.repository.ProcessOperationRepository;
import com.automotive.costing.process.repository.ProcessRoutingRepository;
import com.automotive.costing.quantitystructure.dto.QuantityStructureDtos.EstimateRequest;
import com.automotive.costing.quantitystructure.dto.QuantityStructureDtos.EstimateResponse;
import com.automotive.costing.quantitystructure.service.QuantityStructureCostingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuantityStructureCostingServiceTest {

    @Mock BomNodeRepository bomNodeRepository;
    @Mock ProcessRoutingRepository routingRepository;
    @Mock ProcessOperationRepository operationRepository;
    @Mock MachineMasterRepository machineMasterRepository;
    @Mock LaborMasterRepository laborMasterRepository;
    @Mock MaterialMasterRepository materialMasterRepository;
    @Mock CostingVariantService costingVariantService;

    QuantityStructureCostingService service;

    @BeforeEach
    void setUp() {
        service = new QuantityStructureCostingService(
                bomNodeRepository, routingRepository, operationRepository,
                machineMasterRepository, laborMasterRepository, materialMasterRepository,
                costingVariantService);
    }

    @Test
    void estimate_appliesLotSizeToSetupAmortization() {
        BomNode node = new BomNode();
        node.setBomNodeId("n1");
        node.setMaterialCode("MAT1");
        node.setNetWeight(new BigDecimal("1.0"));
        node.setScrapPct(BigDecimal.ZERO);

        ProcessRouting routing = new ProcessRouting();
        routing.setRoutingId("r1");
        routing.setBomNodeId("n1");
        routing.setStatus(RoutingStatus.DRAFT);
        routing.setCreatedAt(Instant.now());

        ProcessOperation op = new ProcessOperation();
        op.setOperationId("op1");
        op.setRoutingId("r1");
        op.setSequenceNumber(10);
        op.setMachineCode("M1");
        op.setLaborCode("L1");
        op.setCycleTimeSeconds(new BigDecimal("3600")); // 1 hour
        op.setOperatorCount(BigDecimal.ONE);
        op.setSetupTimeSeconds(new BigDecimal("3600")); // 1 hour setup
        op.setBatchSize(new BigDecimal("1"));
        op.setOutputPartsPerCycle(BigDecimal.ONE);

        MachineMaster machine = new MachineMaster();
        machine.setRate(new BigDecimal("100"));
        LaborMaster labor = new LaborMaster();
        labor.setRate(new BigDecimal("50"));
        MaterialMaster mat = new MaterialMaster();
        mat.setBasePrice(new BigDecimal("10"));

        CostingVariant variant = new CostingVariant();
        variant.setVariantId("v1");
        variant.setVariantCode("LOT100");
        variant.setStatus(CostingVariantStatus.ACTIVE);
        variant.setLotSize(new BigDecimal("100"));
        variant.setScrapPct(BigDecimal.ZERO);
        variant.setYieldPct(BigDecimal.ONE);
        variant.setQuantityStructureSource(QuantityStructureSource.BOM_AND_ROUTING);

        when(bomNodeRepository.findByBomNodeId("n1")).thenReturn(Optional.of(node));
        when(costingVariantService.requireActiveForCosting("v1")).thenReturn(variant);
        when(routingRepository.findByBomNodeId("n1")).thenReturn(List.of(routing));
        when(operationRepository.findByRoutingIdOrderBySequenceNumberAsc("r1")).thenReturn(List.of(op));
        when(machineMasterRepository.findFirstByCodeAndStatusOrderByVersionNumberDesc("M1", MasterDataStatus.ACTIVE))
                .thenReturn(Optional.of(machine));
        when(laborMasterRepository.findFirstByCodeAndStatusOrderByVersionNumberDesc("L1", MasterDataStatus.ACTIVE))
                .thenReturn(Optional.of(labor));
        when(materialMasterRepository.findFirstByCodeOrderByVersionNumberDesc("MAT1"))
                .thenReturn(Optional.of(mat));

        EstimateResponse r = service.estimate(new EstimateRequest("n1", "v1", null, null));

        assertEquals(0, new BigDecimal("100.000000").compareTo(r.lotSizeUsed()));
        assertNotNull(r.operations());
        assertEquals(1, r.operations().size());
        // run cost = 100 machine + 50 labor = 150; setup total 150 / lot 100 = 1.5 setup per piece
        assertEquals(0, new BigDecimal("1.500000").compareTo(r.operations().get(0).setupCostPerPiece()));
        assertTrue(r.totalProcessCost().compareTo(new BigDecimal("150")) > 0);
        assertNotNull(r.material());
        assertEquals(0, new BigDecimal("10.000000").compareTo(r.material().materialCost()));
        assertTrue(r.calculationBasis().contains("A2 QS"));
    }
}
