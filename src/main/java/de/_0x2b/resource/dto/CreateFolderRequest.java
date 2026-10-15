package de._0x2b.resource.dto;

import de._0x2b.model.FolderColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateFolderRequest {

    @NotBlank
    @Size(max = 255)
    public String name;

    /** Defaults to {@link FolderColor#F_BASE} when omitted. */
    public FolderColor color;
}
