package de._0x2b.resource.dto;

import de._0x2b.model.FolderColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateFolderRequest {

    @NotBlank
    @Size(max = 255)
    public String name;

    @NotNull
    public FolderColor color;
}
