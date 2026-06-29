package com.sssi.msvcinventory.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetImportRequestDto {

    @NotEmpty(message = "La lista de filas no puede estar vacía")
    private List<AssetImportRowDto> rows;
}
