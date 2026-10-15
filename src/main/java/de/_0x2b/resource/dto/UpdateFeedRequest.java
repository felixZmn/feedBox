package de._0x2b.resource.dto;

import java.net.URI;

import jakarta.validation.constraints.NotNull;

public class UpdateFeedRequest {

    @NotNull
    public URI feedUrl;

    /** Null = unfiled. */
    public Integer folderId;
}
