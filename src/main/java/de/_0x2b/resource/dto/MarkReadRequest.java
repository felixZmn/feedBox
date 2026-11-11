package de._0x2b.resource.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Optional body for bulk mark-as-read. When {@code throughId} is set, the
 * read horizon advances only up to that article id (capped at the current max).
 */
@RegisterForReflection
public record MarkReadRequest(Long throughId) {
}
