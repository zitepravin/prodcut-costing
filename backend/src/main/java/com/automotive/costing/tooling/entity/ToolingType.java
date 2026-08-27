package com.automotive.costing.tooling.entity;

/**
 * TL-01: "parametric tooling cost estimation for at least six tooling types (progressive
 * die, transfer die, injection mold, casting die, forging die, fixture)." Seven types are
 * modeled - the SRS's "fixture" is split into welding fixture and checking fixture per
 * TDD 4.6.3's more granular key-features list, satisfying the >=6 minimum with room to
 * spare rather than collapsing distinct fixture types into one bucket.
 */
public enum ToolingType {
    PROGRESSIVE_DIE,
    TRANSFER_DIE,
    INJECTION_MOLD,
    CASTING_DIE,
    FORGING_DIE,
    WELDING_FIXTURE,
    CHECKING_FIXTURE
}
