package com.automotive.costing.costingvariant;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.costingvariant.dto.CostingVariantDtos.CreateVariantRequest;
import com.automotive.costing.costingvariant.dto.CostingVariantDtos.VariantResponse;
import com.automotive.costing.costingvariant.entity.CostingVariant;
import com.automotive.costing.costingvariant.entity.CostingVariantStatus;
import com.automotive.costing.costingvariant.repository.CostingVariantRepository;
import com.automotive.costing.costingvariant.service.CostingVariantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CostingVariantServiceTest {

    @Mock CostingVariantRepository repository;
    CostingVariantService service;

    @BeforeEach
    void setUp() {
        service = new CostingVariantService(repository);
    }

    @Test
    void create_defaultsAndPersists() {
        when(repository.findByVariantCode("PPC1")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VariantResponse r = service.create(new CreateVariantRequest(
                "PPC1", "Plant production costing",
                null, null, null, null, "EUR",
                new BigDecimal("100"), null, null,
                "OH-FACTORY", null, null, null, null, "Base plant variant"));

        assertEquals("PPC1", r.variantCode());
        assertEquals(CostingVariantStatus.DRAFT, r.status());
        assertEquals("EUR", r.currencyCode());
        assertEquals(0, new BigDecimal("100.000000").compareTo(r.lotSize()));
        assertEquals("OH-FACTORY", r.overheadPoolCode());
    }

    @Test
    void requireActive_rejectsDraft() {
        CostingVariant v = new CostingVariant();
        v.setVariantId("id-1");
        v.setVariantCode("X");
        v.setStatus(CostingVariantStatus.DRAFT);
        when(repository.findById("id-1")).thenReturn(Optional.of(v));
        assertThrows(BusinessRuleViolationException.class, () -> service.requireActiveForCosting("id-1"));
    }

    @Test
    void activate_setsActive() {
        CostingVariant v = new CostingVariant();
        v.setVariantId("id-1");
        v.setVariantCode("X");
        v.setVariantName("X");
        v.setStatus(CostingVariantStatus.DRAFT);
        when(repository.findById("id-1")).thenReturn(Optional.of(v));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        VariantResponse r = service.activate("id-1");
        assertEquals(CostingVariantStatus.ACTIVE, r.status());
    }
}
