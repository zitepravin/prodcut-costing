package com.automotive.costing.purchase.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderStatusTest {

    @Test
    void draftToSubmittedAllowed() {
        assertDoesNotThrow(() ->
                PurchaseOrderStatus.assertTransition("DRAFT", "SUBMITTED"));
    }

    @Test
    void draftToApprovedForbidden() {
        assertThrows(IllegalArgumentException.class, () ->
                PurchaseOrderStatus.assertTransition("DRAFT", "APPROVED"));
    }

    @Test
    void approvalToApprovedAllowed() {
        assertDoesNotThrow(() ->
                PurchaseOrderStatus.assertTransition("APPROVAL", "APPROVED"));
    }

    @Test
    void rejectedToReworkAllowed() {
        assertDoesNotThrow(() ->
                PurchaseOrderStatus.assertTransition("REJECTED", "REWORK"));
    }

    @Test
    void approvedToIssuedAllowed() {
        assertDoesNotThrow(() ->
                PurchaseOrderStatus.assertTransition("APPROVED", "ISSUED"));
    }

    @Test
    void issuedToPartialAndClosedAllowed() {
        assertDoesNotThrow(() ->
                PurchaseOrderStatus.assertTransition("ISSUED", "PARTIALLY_RECEIVED"));
        assertDoesNotThrow(() ->
                PurchaseOrderStatus.assertTransition("ISSUED", "CLOSED"));
        assertDoesNotThrow(() ->
                PurchaseOrderStatus.assertTransition("PARTIALLY_RECEIVED", "CLOSED"));
    }

    @Test
    void closedIsTerminal() {
        assertThrows(IllegalArgumentException.class, () ->
                PurchaseOrderStatus.assertTransition("CLOSED", "ISSUED"));
        assertTrue(PurchaseOrderStatus.isTerminal("CLOSED"));
        assertTrue(PurchaseOrderStatus.isTerminal("CANCELLED"));
        assertFalse(PurchaseOrderStatus.isTerminal("ISSUED"));
    }

    @Test
    void issuedNoLongerFullyTerminalForDraft() {
        assertThrows(IllegalArgumentException.class, () ->
                PurchaseOrderStatus.assertTransition("ISSUED", "DRAFT"));
    }

    @Test
    void editableOnlyDraftOrRework() {
        assertTrue(PurchaseOrderStatus.isEditable("DRAFT"));
        assertTrue(PurchaseOrderStatus.isEditable("REWORK"));
        assertFalse(PurchaseOrderStatus.isEditable("APPROVED"));
        assertFalse(PurchaseOrderStatus.isEditable("ISSUED"));
        assertFalse(PurchaseOrderStatus.isEditable("CLOSED"));
    }
}
