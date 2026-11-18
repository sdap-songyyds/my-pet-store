package cn.edu.csu.mycoursestore.admin.service;

import cn.edu.csu.mycoursestore.admin.dto.AdminProductDetail;
import cn.edu.csu.mycoursestore.admin.dto.AdminProductFilter;
import cn.edu.csu.mycoursestore.admin.dto.AdminProductRow;
import cn.edu.csu.mycoursestore.admin.dto.CategoryGroup;
import cn.edu.csu.mycoursestore.admin.dto.CategoryOption;
import cn.edu.csu.mycoursestore.admin.dto.ProductCreateRequest;
import cn.edu.csu.mycoursestore.admin.dto.ProductImageRequest;
import cn.edu.csu.mycoursestore.admin.dto.ProductUpdateRequest;
import cn.edu.csu.mycoursestore.admin.paging.PageQuery;
import cn.edu.csu.mycoursestore.admin.paging.PageResult;
import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.product.entity.Category;
import cn.edu.csu.mycoursestore.product.entity.Product;
import cn.edu.csu.mycoursestore.product.entity.ProductImage;
import cn.edu.csu.mycoursestore.product.entity.ProductStatus;
import cn.edu.csu.mycoursestore.product.mapper.CategoryMapper;
import cn.edu.csu.mycoursestore.product.mapper.ProductImageMapper;
import cn.edu.csu.mycoursestore.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminProductService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ProductImageMapper productImageMapper;

    public AdminProductService(ProductMapper productMapper,
                               CategoryMapper categoryMapper,
                               ProductImageMapper productImageMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.productImageMapper = productImageMapper;
    }

    public List<CategoryGroup> categoryGroups() {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
        Map<Long, List<Category>> childrenByParent = categories.stream()
                .filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        List<CategoryGroup> groups = new ArrayList<>();
        for (Category category : categories) {
            if (category.getParentId() == null) {
                List<CategoryOption> children = childrenByParent.getOrDefault(category.getId(), List.of()).stream()
                        .map(child -> new CategoryOption(child.getId(), child.getName()))
                        .toList();
                groups.add(new CategoryGroup(category.getName(), children));
            }
        }
        return groups;
    }

    public PageResult<AdminProductRow> list(AdminProductFilter filter, PageQuery page) {
        List<Long> categoryIds = resolveCategoryIds(filter.categoryId());
        if (categoryIds != null && categoryIds.isEmpty()) {
            return PageResult.empty(page);
        }

        long total = productMapper.selectCount(buildWrapper(filter, categoryIds));
        if (total == 0) {
            return PageResult.empty(page);
        }

        List<Product> products = productMapper.selectList(buildWrapper(filter, categoryIds)
                .orderByAsc(Product::getSortOrder)
                .orderByAsc(Product::getId)
                .last("LIMIT " + page.size() + " OFFSET " + page.offset()));

        Map<Long, Category> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        List<AdminProductRow> rows = products.stream()
                .map(product -> new AdminProductRow(
                        product.getId(),
                        product.getSku(),
                        product.getName(),
                        categoryPath(product.getCategoryId(), categoryMap),
                        product.getCoverImage(),
                        product.getPrice(),
                        product.getStock() == null ? 0 : product.getStock(),
                        product.getStatus(),
                        statusLabel(product.getStatus()),
                        product.getSortOrder() == null ? 0 : product.getSortOrder(),
                        product.getVersion() == null ? 0 : product.getVersion()))
                .toList();
        return new PageResult<>(rows, page.page(), page.size(), total);
    }

    public AdminProductDetail detail(long id) {
        Product product = requireProduct(id);
        List<AdminProductDetail.GalleryImage> images = productImageMapper.selectList(
                        new LambdaQueryWrapper<ProductImage>()
                                .eq(ProductImage::getProductId, id)
                                .orderByAsc(ProductImage::getSortOrder)
                                .orderByAsc(ProductImage::getId))
                .stream()
                .map(image -> new AdminProductDetail.GalleryImage(
                        image.getId(),
                        image.getImagePath(),
                        image.getAltText(),
                        image.getSortOrder() == null ? 0 : image.getSortOrder()))
                .toList();

        return new AdminProductDetail(
                product.getId(),
                product.getSku(),
                product.getCategoryId(),
                categoryPath(product.getCategoryId()),
                product.getName(),
                product.getSummary(),
                product.getDescription(),
                product.getCoverImage(),
                product.getPrice(),
                product.getStock() == null ? 0 : product.getStock(),
                product.getStatus(),
                statusLabel(product.getStatus()),
                product.getSortOrder() == null ? 0 : product.getSortOrder(),
                product.getVersion() == null ? 0 : product.getVersion(),
                format(product.getCreatedAt()),
                format(product.getUpdatedAt()),
                images);
    }

    @Transactional
    public long create(ProductCreateRequest request) {
        requireLeafCategory(request.categoryId());
        String sku = request.sku().trim();
        if (productMapper.exists(new LambdaQueryWrapper<Product>().eq(Product::getSku, sku))) {
            throw skuTaken();
        }

        Product product = new Product();
        product.setSku(sku);
        product.setCategoryId(request.categoryId());
        product.setName(request.name().trim());
        product.setSummary(blankToNull(request.summary()));
        product.setDescription(blankToNull(request.description()));
        product.setCoverImage(request.coverImage().trim());
        product.setPrice(request.price().setScale(2, RoundingMode.HALF_UP));
        product.setStock(request.stock());
        product.setStatus(ProductStatus.DRAFT);
        product.setSortOrder(request.sortOrder());
        product.setVersion(0L);
        try {
            productMapper.insert(product);
        } catch (DuplicateKeyException e) {
            throw skuTaken();
        }
        return product.getId();
    }

    @Transactional
    public void update(long id, ProductUpdateRequest request) {
        requireProduct(id);
        int rows = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id)
                .eq(Product::getVersion, request.version())
                .set(Product::getName, request.name().trim())
                .set(Product::getSummary, blankToNull(request.summary()))
                .set(Product::getDescription, blankToNull(request.description()))
                .set(Product::getCoverImage, request.coverImage().trim())
                .set(Product::getPrice, request.price().setScale(2, RoundingMode.HALF_UP))
                .set(Product::getSortOrder, request.sortOrder())
                .setSql("version = version + 1, updated_at = LOCALTIMESTAMP"));
        if (rows == 0) {
            throw productConflict();
        }
    }

    @Transactional
    public void changeStatus(long id, ProductStatus target, long version) {
        if (target == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "商品状态不合法");
        }
        requireProduct(id);
        int rows = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id)
                .eq(Product::getVersion, version)
                .set(Product::getStatus, target)
                .setSql("version = version + 1, updated_at = LOCALTIMESTAMP"));
        if (rows == 0) {
            throw productConflict();
        }
    }

    @Transactional
    public void addImage(long productId, ProductImageRequest request) {
        requireProduct(productId);
        ProductImage image = new ProductImage();
        image.setProductId(productId);
        image.setImagePath(request.imagePath().trim());
        image.setAltText(blankToNull(request.altText()));
        image.setSortOrder(request.sortOrder());
        productImageMapper.insert(image);
    }

    @Transactional
    public void updateImage(long productId, long imageId, ProductImageRequest request) {
        int rows = productImageMapper.update(null, new LambdaUpdateWrapper<ProductImage>()
                .eq(ProductImage::getId, imageId)
                .eq(ProductImage::getProductId, productId)
                .set(ProductImage::getImagePath, request.imagePath().trim())
                .set(ProductImage::getAltText, blankToNull(request.altText()))
                .set(ProductImage::getSortOrder, request.sortOrder()));
        if (rows == 0) {
            throw imageNotFound();
        }
    }

    @Transactional
    public void deleteImage(long productId, long imageId) {
        int rows = productImageMapper.delete(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getId, imageId)
                .eq(ProductImage::getProductId, productId));
        if (rows == 0) {
            throw imageNotFound();
        }
    }

    private LambdaQueryWrapper<Product> buildWrapper(AdminProductFilter filter, List<Long> categoryIds) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (filter.status() != null) {
            wrapper.eq(Product::getStatus, filter.status());
        }
        if (categoryIds != null) {
            wrapper.in(Product::getCategoryId, categoryIds);
        }
        String keyword = trimToNull(filter.keyword());
        if (keyword != null) {
            wrapper.and(inner -> inner.like(Product::getName, keyword)
                    .or()
                    .like(Product::getSku, keyword));
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

    private Category requireLeafCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || category.getParentId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY", "商品必须归属二级分类");
        }
        return category;
    }

    private Product requireProduct(long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "商品不存在");
        }
        return product;
    }

    private String categoryPath(Long categoryId) {
        if (categoryId == null) {
            return "未分类";
        }
        Map<Long, Category> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        return categoryPath(categoryId, categoryMap);
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

    private static String format(LocalDateTime time) {
        return time == null ? "-" : time.format(TIME_FORMAT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String blankToNull(String value) {
        return trimToNull(value);
    }

    private static ApiException skuTaken() {
        return new ApiException(HttpStatus.CONFLICT, "SKU_TAKEN", "SKU 已存在");
    }

    private static ApiException productConflict() {
        return new ApiException(HttpStatus.CONFLICT, "PRODUCT_CONFLICT", "商品已被修改，请刷新后重试");
    }

    private static ApiException imageNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "图片不存在或已被删除");
    }
}
