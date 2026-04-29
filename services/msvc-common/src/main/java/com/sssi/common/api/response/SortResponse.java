package com.sssi.common.api.response;

import lombok.*;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SortResponse {

    private boolean sorted;
    private boolean unsorted;
    private boolean empty;

    public static SortResponse from(Sort sort) {
        return SortResponse.builder()
                .sorted(sort.isSorted())
                .unsorted(sort.isUnsorted())
                .empty(sort.isEmpty())
                .build();
    }
}