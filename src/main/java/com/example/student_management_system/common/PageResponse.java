package com.example.student_management_system.common;

import java.util.List;

/** One page of results plus the numbers the frontend needs for "Page 2 of 5". page is 0-based. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int totalPages = (int) Math.ceil(all.size() / (double) safeSize);
        int safePage = Math.max(0, page);
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PageResponse<>(List.copyOf(all.subList(from, to)), safePage, safeSize, all.size(), totalPages);
    }
}
