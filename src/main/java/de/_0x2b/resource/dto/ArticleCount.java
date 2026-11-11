package de._0x2b.resource.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

/** Cap-aware count of articles newer than a seen id. */
@RegisterForReflection
public record ArticleCount(int count) {
}
