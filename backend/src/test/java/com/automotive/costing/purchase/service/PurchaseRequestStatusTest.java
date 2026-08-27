package com.automotive.costing.purchase.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseRequestStatusTest {

    @Test
    void draftToSubmittedAllowed() {
        assertDoesNotThrow(() ->
                PurchaseRequestStatus.assertTransition("DRAFT", "SUBMITTED"));
    }

    @Test
    void draftToApprovedForbidden() {
        assertThrows(IllegalArgumentException.class, () ->
                PurchaseRequestStatus.assertTransition("DRAFT", "APPROVED"));
    }

    @Test
    void rejectedToDraftAllowed() {
        assertDoesNotThrow(() ->
                PurchaseRequestStatus.assertTransition("REJECTED", "DRAFT"));
    }

    @Test
    void releasedIsTerminal() {
        assertThrows(IllegalArgumentException.class, () ->
                PurchaseRequestStatus.assertTransition("RELEASED", "DRAFT"));
    }
}
