package com.automotive.costing.versioning.dto;

import com.automotive.costing.versioning.entity.ChangeTrigger;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public final class VersionControlDtos {

    private VersionControlDtos() {
    }

    public record CreateSnapshotRequest(
            @NotBlank String entityType,
            @NotBlank String entityId,
            @NotNull ChangeTrigger changeTrigger,
            String changeReference,
            String changeComment,
            /** Optional client-supplied snapshot; if null, service loads a minimal server-side snapshot. */
            Map<String, Object> snapshot
    ) {
    }

    public record VersionResponse(
            String versionId,
            String entityType,
            String entityId,
            int versionNumber,
            ChangeTrigger changeTrigger,
            String changeReference,
            String changeComment,
            boolean programBaseline,
            String createdBy,
            Instant createdAt,
            Map<String, Object> snapshot
    ) {
    }

    public record TimelineEntry(
            String versionId,
            int versionNumber,
            ChangeTrigger changeTrigger,
            String changeReference,
            String changeComment,
            boolean programBaseline,
            String createdBy,
            Instant createdAt
    ) {
    }

    public record CompareResponse(
            String entityType,
            String entityId,
            VersionResponse left,
            VersionResponse right
    ) {
    }
}
