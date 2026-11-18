package cn.edu.csu.mycoursestore.admin.service;

import cn.edu.csu.mycoursestore.admin.dto.AdminInventoryFilter;
import cn.edu.csu.mycoursestore.admin.dto.AdminInventoryRow;
import cn.edu.csu.mycoursestore.admin.dto.InventoryUpdateRequest;
import cn.edu.csu.mycoursestore.admin.dto.StockLevel;
import cn.edu.csu.mycoursestore.admin.paging.PageQuery;
import cn.edu.csu.mycoursestore.admin.paging.PageResult;
import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.product.entity.Category;
import cn.edu.csu.mycoursestore.product.entity.Product;
import cn.edu.csu.mycoursestore.product.entity.ProductStatus;
import cn.edu.csu.mycoursestore.product.mapper.CategoryMapper;
import cn.edu.csu.mycoursestore.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminInventoryService {

    public static final int LOW_STOCK_THRESHOLD = 10;

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    public AdminInventoryService(ProductMapper productMapper, CategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
    }

    public PageResult<AdminInventoryRow> list(AdminInventoryFilter filter, PageQuery page) {
        List<Long> categoryIds = resolveCategoryIds(filter.categoryId());
        if (categoryIds != null && categoryIds.isEmpty()) {
            return PageResult.empty(page);
        }

        long total = productMapper.selectCount(buildWrapper(filter, categoryIds));
        if (total == 0) {
            return PageResult.empty(page);
        }

        List<Product> products = productMapper.selectList(buildWrapper(filter, categoryIds)
                .orderByAsc(Product::getStock)
                .orderByAsc(Product::getId)
                .last("LIMIT " + page.size() + " OFFSET " + page.offset()));

        Map<Long, Category> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        List<AdminInventoryRow> rows = products.stream()
                .map(product -> new AdminInventoryRow(
                        product.getId(),
                        product.getSku(),
                        product.getName(),
                        categoryPath(product.getCategoryId(), categoryMap),
                        product.getCoverImage(),
                        product.getPrice(),
                        product.getStock() == null ? 0 : product.getStock(),
                        product.getStatus(),
                        statusLabel(product.getStatus()),
                        product.getVersion() == null ? 0 : product.getVersion()))
                .toList();
        return new PageResult<>(rows, page.page(), page.size(), total);
    }

    @Transactional
    public void updateStock(long id, InventoryUpdateRequest request) {
        if (productMapper.selectById(id) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "商品不存在");
        }
        int rows = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id)
                .eq(Product::getVersion, request.version())
                .set(Product::getStock, request.stock())
                .setSql("version = version + 1, updated_at = LOCALTIMESTAMP"));
        if (rows == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "STOCK_CONFLICT", "库存已被修改，请刷新后重试");
        }
    }

    private LambdaQueryWrapper<Product> buildWrapper(AdminInventoryFilter filter, List<Long> categoryIds) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (categoryIds != null) {
            wrapper.in(Product::getCategoryId, categoryIds);
        }
        String keyword = trimToNull(filter.keyword());
        if (keyword != null) {
            wrapper.and(inner -> inner.like(Product::getName, keyword)
                    .or()
                    .like(Product::getSku, keyword));
        }
        if (filter.level() == StockLevel.OUT_OF_STOCK) {
            wrapper.eq(Product::getStock, 0);
        } else if (filter.level() == StockLevel.LOW_STOCK) {
            wrapper.gt(Product::getStock, 0).le(Product::getStock, LOW_STOCK_THRESHOLD);
        }
        return wrapper;
    }

    private List<Long> resolveCategoryIds(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            return List.of();
        }
        if (category.getParentId() != null) {
            return List.of(categoryId);
        }
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, categoryId))
                .stream()
                .map(Category::getId)
                .toList();
    }

    private static String categoryPath(Long categoryId, Map<Long, Category> categoryMap) {
        if (categoryId == null) {
            return "未分类";
        }
        Category category = categoryMap.get(categoryId);
        if (category == null) {
            return "未分类";
        }
        if (category.getParentId() == null) {
            return category.getName();
        }
        Category parent = categoryMap.get(category.getParentId());
        return parent == null ? category.getName() : parent.getName() + " / " + category.getName();
    }

    private static String statusLabel(ProductStatus status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case DRAFT -> "草稿";
            case ON_SALE -> "在售";
        };
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
