package com.automotive.costing.common.event;

/**
 * Published by {@code MaterialMasterService} whenever a new version of a material rate
 * is published (MDM-02 create/new-version), so that MAT-05 ("System shall recalculate
 * all dependent BOM and part costs automatically when a material master rate is updated
 * and published") can react without the masterdata package taking a compile-time
 * dependency on the material-costing or bom packages - listeners live downstream and
 * subscribe to this event via Spring's {@code ApplicationEventPublisher}/{@code
 * @EventListener}, keeping the Master &amp; Reference Data service group (TDD Section 3.1)
 * decoupled from the Costing Engine and Product Structure service groups it feeds.
 *
 * @param materialCode the material_master code whose rate changed
 * @param recordId     the new version's record_id
 * @param newRate      the new effective rate (fixed-price basis; ignored for index-linked
 *                     materials, whose effective rate is computed at calculation time)
 */
public record MaterialRatePublishedEvent(String materialCode, String recordId, java.math.BigDecimal newRate) {
}
