package com.automotive.costing.security;

import com.automotive.costing.platform.user.service.PermissionMatrixService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase3PermissionWiringTest {

    PermissionMatrixService matrix;

    @BeforeEach
    void setUp() {
        matrix = new PermissionMatrixService();
    }

    @Test
    void costingEngineer_hasBomCreate_notRfqDelete() {
        assertTrue(matrix.hasPermission(List.of("COSTING_ENGINEER"), "BOM_CREATE"));
        assertTrue(matrix.hasPermission(List.of("COSTING_ENGINEER"), "BOM_VIEW"));
        assertTrue(matrix.hasPermission(List.of("COSTING_ENGINEER"), "SHOULD_COST"));
        assertFalse(matrix.hasPermission(List.of("COSTING_ENGINEER"), "RFQ_DELETE"));
    }

    @Test
    void purchasingBuyer_hasRfqCreate_notBomCreate() {
        assertTrue(matrix.hasPermission(List.of("PURCHASING_BUYER"), "RFQ_CREATE"));
        assertTrue(matrix.hasPermission(List.of("PURCHASING_BUYER"), "RFQ_VIEW"));
        assertFalse(matrix.hasPermission(List.of("PURCHASING_BUYER"), "BOM_CREATE"));
    }

    @Test
    void viewer_readOnly_bomAndRfq() {
        assertTrue(matrix.hasPermission(List.of("VIEWER"), "BOM_VIEW"));
        assertTrue(matrix.hasPermission(List.of("VIEWER"), "RFQ_VIEW"));
        assertFalse(matrix.hasPermission(List.of("VIEWER"), "BOM_CREATE"));
        assertFalse(matrix.hasPermission(List.of("VIEWER"), "RFQ_CREATE"));
    }

    @Test
    void systemAdmin_hasFullBomRfqCosting() {
        List<String> roles = List.of("SYSTEM_ADMINISTRATOR");
        assertTrue(matrix.hasPermission(roles, "BOM_DELETE"));
        assertTrue(matrix.hasPermission(roles, "RFQ_DELETE"));
        assertTrue(matrix.hasPermission(roles, "COSTING_APPROVE"));
        assertTrue(matrix.hasPermission(roles, "SHOULD_COST"));
    }
}
