package com.sssi.common.api.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageableResponse {

    private int pageNumber;
    private int pageSize;
    private long offset;
    private boolean paged;
    private boolean unpaged;

    private SortResponse sort;
}