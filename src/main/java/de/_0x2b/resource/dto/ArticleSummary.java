package de._0x2b.resource.dto;

import java.time.Instant;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Lightweight article row for list/pagination endpoints. Omits
 * {@code content}/{@code description} bodies — the reader loads the full
 * article via {@code GET /api/article/{id}}.
 */
@RegisterForReflection
public record ArticleSummary(
        long id,
        int feedId,
        String feedName,
        String title,
        String link,
        Instant published,
        String authors,
        String imageUrl,
        boolean read,
        boolean saved) {
}
