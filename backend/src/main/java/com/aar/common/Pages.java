package com.aar.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class Pages {

    public static final int MAX_SIZE = 100;

    private Pages() {
    }

    public static PageRequest of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_SIZE), sort);
    }
}
