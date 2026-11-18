package cn.edu.csu.mycoursestore.admin.paging;

/**
 * 后台分页参数。页码从 1 开始，单页条数限制在 1-50，避免手写 limit/offset 时出现非法值。
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    public PageQuery {
        page = Math.max(page, 1);
        size = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }

    public long offset() {
        return (long) (page - 1) * size;
    }
}
