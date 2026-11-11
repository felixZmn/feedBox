package de._0x2b.resource.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * JSON body of every API error response. {@code code} is a stable,
 * machine-readable key the SPA maps to its own copy; {@code message} is a
 * short human-readable fallback and never carries stack traces or SQL.
 */
@RegisterForReflection
public record ApiError(String code, String message) {
}
