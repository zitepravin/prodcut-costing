package com.automotive.costing.vendorselection.service;

import com.automotive.costing.vendorselection.entity.ScorecardTemplateStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P3-07a policy constants / status model.
 */
class ScorecardP307aPolicyTest {

    @Test
    void statusIncludesDraftPublishedRetired() {
        assertNotNull(ScorecardTemplateStatus.DRAFT);
        assertNotNull(ScorecardTemplateStatus.ACTIVE);
        assertNotNull(ScorecardTemplateStatus.INACTIVE);
    }

    @Test
    void publishedMeaningIsActive() {
        // ACTIVE == published usable for evaluation (P3-07a)
        assertEquals("ACTIVE", ScorecardTemplateStatus.ACTIVE.name());
    }
}
