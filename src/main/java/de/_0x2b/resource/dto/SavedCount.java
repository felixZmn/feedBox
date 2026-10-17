package de._0x2b.resource.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

/** Dated saved-article total for the sidebar badge. */
@RegisterForReflection
public record SavedCount(int count) {
}
