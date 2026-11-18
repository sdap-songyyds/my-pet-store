package cn.edu.csu.mycoursestore.admin.service;

import cn.edu.csu.mycoursestore.admin.dto.AdminCategoryDetail;
import cn.edu.csu.mycoursestore.admin.dto.AdminCategoryGroup;
import cn.edu.csu.mycoursestore.admin.dto.AdminCategoryRow;
import cn.edu.csu.mycoursestore.admin.dto.CategoryCreateRequest;
import cn.edu.csu.mycoursestore.admin.dto.CategoryUpdateRequest;
import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.product.entity.Category;
import cn.edu.csu.mycoursestore.product.entity.Product;
import cn.edu.csu.mycoursestore.product.mapper.CategoryMapper;
import cn.edu.csu.mycoursestore.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminCategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    public AdminCategoryService(CategoryMapper categoryMapper, ProductMapper productMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
    }

    public List<AdminCategoryGroup> groups() {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
        Map<Long, Integer> productCounts = productCountsByCategory();
        Map<Long, List<Category>> childrenByParent = categories.stream()
                .filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        List<AdminCategoryGroup> groups = new ArrayList<>();
        for (Category parent : categories) {
            if (parent.getParentId() != null) {
                continue;
            }
            List<Category> children = childrenByParent.getOrDefault(parent.getId(), List.of());
            AdminCategoryRow parentRow = row(parent, children.size(),
                    productCounts.getOrDefault(parent.getId(), 0));
            List<AdminCategoryRow> childRows = children.stream()
                    .map(child -> row(child, 0, productCounts.getOrDefault(child.getId(), 0)))
                    .toList();
            groups.add(new AdminCategoryGroup(parentRow, childRows));
        }
        return groups;
    }

    public AdminCategoryDetail detail(long id) {
        Category category = requireCategory(id);
        String parentName = null;
        if (category.getParentId() != null) {
            Category parent = categoryMapper.selectById(category.getParentId());
            parentName = parent == null ? "未知父分类" : parent.getName();
        }
        return new AdminCategoryDetail(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getParentId(),
                parentName,
                category.getSortOrder() == null ? 0 : category.getSortOrder());
    }

    public AdminCategoryDetail requireFirstLevel(long id) {
        AdminCategoryDetail detail = detail(id);
        if (detail.parentId() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARENT", "只能在第一级分类下新增二级分类");
        }
        return detail;
    }

    @Transactional
    public long create(CategoryCreateRequest request) {
        Long parentId = request.parentId();
        if (parentId != null) {
            Category parent = requireCategory(parentId);
            if (parent.getParentId() != null) {
                throw invalidParent();
            }
        }

        String code = request.code().trim();
        String name = request.name().trim();
        if (categoryMapper.exists(new LambdaQueryWrapper<Category>().eq(Category::getCode, code))) {
            throw codeTaken();
        }
        if (siblingNameExists(parentId, name, null)) {
            throw siblingNameTaken();
        }

        Category category = new Category();
        category.setCode(code);
        category.setParentId(parentId);
        category.setName(name);
        category.setSortOrder(request.sortOrder());
        try {
            categoryMapper.insert(category);
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_DUPLICATE", "分类编码或同级分类名称已存在");
        }
        return category.getId();
    }

    @Transactional
    public void update(long id, CategoryUpdateRequest request) {
        Category category = requireCategory(id);
        String name = request.name().trim();
        if (siblingNameExists(category.getParentId(), name, id)) {
            throw siblingNameTaken();
        }
        int rows = categoryMapper.update(null, new LambdaUpdateWrapper<Category>()
                .eq(Category::getId, id)
                .set(Category::getName, name)
                .set(Category::getSortOrder, request.sortOrder())
                .setSql("updated_at = LOCALTIMESTAMP"));
        if (rows == 0) {
            throw notFound();
        }
    }

    @Transactional
    public void delete(long id) {
        Category category = requireCategory(id);
        if (category.getParentId() == null) {
            long children = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                    .eq(Category::getParentId, id));
            if (children > 0) {
                throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_HAS_CHILDREN",
                        "该分类仍有 " + children + " 个二级分类，不能删除");
            }
        }

        long products = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (products > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_HAS_PRODUCTS",
                    "该分类仍有 " + products + " 个商品，不能删除");
        }

        try {
            if (categoryMapper.deleteById(id) == 0) {
                throw notFound();
            }
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_DELETE_CONFLICT",
                    "分类仍被引用，请刷新后重试");
        }
    }

    private AdminCategoryRow row(Category category, int childCount, int productCount) {
        return new AdminCategoryRow(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getParentId(),
                category.getSortOrder() == null ? 0 : category.getSortOrder(),
                childCount,
                productCount,
                blockedReason(childCount, productCount));
    }

    private Map<Long, Integer> productCountsByCategory() {
        Map<Long, Integer> counts = new HashMap<>();
        productMapper.selectList(new LambdaQueryWrapper<Product>().select(Product::getCategoryId))
                .forEach(product -> {
                    if (product.getCategoryId() != null) {
                        counts.merge(product.getCategoryId(), 1, Integer::sum);
                    }
                });
        return counts;
    }

    private boolean siblingNameExists(Long parentId, String name, Long excludeId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name);
        if (parentId == null) {
            wrapper.isNull(Category::getParentId);
        } else {
            wrapper.eq(Category::getParentId, parentId);
        }
        if (excludeId != null) {
            wrapper.ne(Category::getId, excludeId);
        }
        return categoryMapper.exists(wrapper);
    }

    private Category requireCategory(long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw notFound();
        }
        return category;
    }

    private static String blockedReason(int childCount, int productCount) {
        if (childCount == 0 && productCount == 0) {
            return null;
        }
        List<String> reasons = new ArrayList<>(2);
        if (childCount > 0) {
            reasons.add("仍有 " + childCount + " 个二级分类");
        }
        if (productCount > 0) {
            reasons.add("仍有 " + productCount + " 个商品");
        }
        return String.join("，", reasons);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "分类不存在");
    }

    private static ApiException invalidParent() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARENT", "只能在第一级分类下新增二级分类");
    }

    private static ApiException codeTaken() {
        return new ApiException(HttpStatus.CONFLICT, "CATEGORY_CODE_TAKEN", "分类编码已存在");
    }

    private static ApiException siblingNameTaken() {
        return new ApiException(HttpStatus.CONFLICT, "CATEGORY_NAME_TAKEN", "同一层级下已存在同名分类");
    }
}
