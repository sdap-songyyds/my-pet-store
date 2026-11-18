package cn.edu.csu.mycoursestore.admin.paging;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long total) {

    public static <T> PageResult<T> empty(PageQuery query) {
        return new PageResult<>(List.of(), query.page(), query.size(), 0);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) ((total + size - 1) / size);
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages();
    }
}
