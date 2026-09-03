package com.proseg.common.api.util;

import com.proseg.common.api.response.*;
import org.springframework.data.domain.Page;

public class PageMapper {

    public static <T> PageResponse<T> from(Page<T> page) {

        SortResponse sort = SortResponse.from(page.getSort());

        PageableResponse pageable = PageableResponse.builder()
                .pageNumber(page.getPageable().getPageNumber())
                .pageSize(page.getPageable().getPageSize())
                .offset(page.getPageable().getOffset())
                .paged(page.getPageable().isPaged())
                .unpaged(page.getPageable().isUnpaged())
                .sort(SortResponse.from(page.getPageable().getSort()))
                .build();

        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageable(pageable)
                .last(page.isLast())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .size(page.getSize())
                .number(page.getNumber())
                .sort(sort)
                .first(page.isFirst())
                .numberOfElements(page.getNumberOfElements())
                .empty(page.isEmpty())
                .build();
    }
}