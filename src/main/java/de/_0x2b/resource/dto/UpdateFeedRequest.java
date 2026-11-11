package de._0x2b.resource.dto;

import java.net.URI;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateFeedRequest {

    @NotNull
    public URI feedUrl;

    /** Null = unfiled. */
    public Integer folderId;

    /** Null or blank keeps the current name. */
    @Size(max = 255)
    public String name;
}
