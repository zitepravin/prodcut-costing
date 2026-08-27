package com.automotive.costing.security;

import com.automotive.costing.fta.controller.FtaController;
import com.automotive.costing.importcosting.controller.ImportCostingController;
import com.automotive.costing.universal.controller.UniversalCostingController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** V286 — every mapped method on FTA/Import/Universal must declare @PreAuthorize. */
class FineGrainedRbacAnnotationTest {

    @Test void ftaController_everyMappedMethodHasPreAuthorize() {
        assertEveryMappedMethodHasPreAuthorize(FtaController.class);
    }
    @Test void importController_everyMappedMethodHasPreAuthorize() {
        assertEveryMappedMethodHasPreAuthorize(ImportCostingController.class);
    }
    @Test void universalController_everyMappedMethodHasPreAuthorize() {
        assertEveryMappedMethodHasPreAuthorize(UniversalCostingController.class);
    }
    @Test void fta_viewerCannotApproveOrLock() throws Exception {
        Method approve = FtaController.class.getMethod("approve", String.class, java.util.Map.class);
        Method lock = FtaController.class.getMethod("lock", String.class);
        assertFalse(preAuthorizeAllows(approve, "VIEWER"));
        assertFalse(preAuthorizeAllows(lock, "VIEWER"));
        assertTrue(preAuthorizeAllows(approve, "COSTING_MANAGER"));
        assertTrue(preAuthorizeAllows(lock, "COSTING_MANAGER"));
    }
    @Test void fta_engineerCanAnalyzeButNotApprove() throws Exception {
        Method analyze = FtaController.class.getMethod("analyze",
                Class.forName("com.automotive.costing.fta.dto.FtaDtos$AnalyzeRequest"));
        Method approve = FtaController.class.getMethod("approve", String.class, java.util.Map.class);
        assertTrue(preAuthorizeAllows(analyze, "COSTING_ENGINEER"));
        assertFalse(preAuthorizeAllows(approve, "COSTING_ENGINEER"));
    }
    @Test void import_viewerCannotCalculateOrDelete() throws Exception {
        Method calculate = ImportCostingController.class.getMethod("calculate",
                Class.forName("com.automotive.costing.importcosting.dto.ImportCostingDtos$CalculateRequest"));
        Method delete = ImportCostingController.class.getMethod("delete", String.class);
        assertFalse(preAuthorizeAllows(calculate, "VIEWER"));
        assertFalse(preAuthorizeAllows(delete, "VIEWER"));
        assertTrue(preAuthorizeAllows(calculate, "COSTING_ENGINEER"));
        assertTrue(preAuthorizeAllows(delete, "COSTING_MANAGER"));
    }
    @Test void universal_viewerCanListButNotCreate() throws Exception {
        Method list = UniversalCostingController.class.getMethod("listCostObjects");
        Method create = UniversalCostingController.class.getMethod("createCostObject",
                Class.forName("com.automotive.costing.universal.dto.UniversalCostingDtos$CostObjectRequest"));
        assertTrue(preAuthorizeAllows(list, "VIEWER"));
        assertFalse(preAuthorizeAllows(create, "VIEWER"));
        assertTrue(preAuthorizeAllows(create, "COSTING_ENGINEER"));
    }
    @Test void universal_onlyManagerCanApproveNorm() throws Exception {
        Method approve = UniversalCostingController.class.getMethod("approveNorm", String.class, String.class);
        assertFalse(preAuthorizeAllows(approve, "VIEWER"));
        assertFalse(preAuthorizeAllows(approve, "COSTING_ENGINEER"));
        assertTrue(preAuthorizeAllows(approve, "COSTING_MANAGER"));
        assertTrue(preAuthorizeAllows(approve, "FINANCE_CONTROLLER"));
        assertTrue(preAuthorizeAllows(approve, "ADMIN"));
    }

    private static void assertEveryMappedMethodHasPreAuthorize(Class<?> controller) {
        Set<String> missing = new HashSet<>();
        for (Method m : controller.getDeclaredMethods()) {
            boolean mapped = m.isAnnotationPresent(GetMapping.class)
                    || m.isAnnotationPresent(PostMapping.class)
                    || m.isAnnotationPresent(DeleteMapping.class)
                    || m.isAnnotationPresent(RequestMapping.class);
            if (mapped && !m.isAnnotationPresent(PreAuthorize.class)) {
                missing.add(m.getName());
            }
        }
        assertTrue(missing.isEmpty(), controller.getSimpleName() + " missing PreAuthorize: " + missing);
    }

    private static boolean preAuthorizeAllows(Method m, String role) {
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        assertNotNull(pa, m.getName() + " missing PreAuthorize");
        return Arrays.stream(pa.value().split("[,()']+")).map(String::trim).anyMatch(t -> t.equals(role));
    }
}
