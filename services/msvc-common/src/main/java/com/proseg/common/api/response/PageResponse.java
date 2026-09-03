package com.proseg.common.api.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> content;

    private PageableResponse pageable;

    private boolean last;
    private long totalElements;
    private int totalPages;
    private int size;
    private int number;

    private SortResponse sort;

    private int numberOfElements;
    private boolean first;
    private boolean empty;
}