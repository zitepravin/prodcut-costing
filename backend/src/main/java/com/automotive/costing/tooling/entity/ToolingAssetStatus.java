package com.automotive.costing.tooling.entity;

/** TL-07: asset register lifecycle status. RETIRED tools stay in the register (never deleted - AUD-02/VER-01's "never delete, only deactivate" convention applied here too), just excluded from active piece-price calculations. */
public enum ToolingAssetStatus {
    ACTIVE,
    RETIRED
}
