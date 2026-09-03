package com.proseg.msvc_transport.dto.cleaning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleaningRowDto {
    private int rowIndex;
    @Builder.Default
    private Map<String, String> values = new LinkedHashMap<>();
}
