package com.experimentops.platformapi.service;

import com.experimentops.common.exceptions.runtime.ValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static com.experimentops.common.exceptions.constant.ErrorCode.INVALID_INPUTS;

final class PaginationUtil {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private PaginationUtil() {
    }

    static Pageable createPageRequest(Integer page, Integer size) {
        int pageNumber = page == null ? DEFAULT_PAGE : page;
        int pageSize = size == null ? DEFAULT_SIZE : size;

        if (pageNumber < 0) {
            throw new ValidationException(INVALID_INPUTS, "Page must be greater than or equal to 0");
        }
        if (pageSize < 1) {
            throw new ValidationException(INVALID_INPUTS, "Size must be greater than or equal to 1");
        }

        return PageRequest.of(pageNumber, pageSize);
    }
}
