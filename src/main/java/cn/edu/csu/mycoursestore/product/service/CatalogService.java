package cn.edu.csu.mycoursestore.product.service;

import cn.edu.csu.mycoursestore.product.dto.CategoryNode;
import cn.edu.csu.mycoursestore.product.dto.ProductCard;
import cn.edu.csu.mycoursestore.product.dto.ProductDetail;
import cn.edu.csu.mycoursestore.product.entity.Category;
import cn.edu.csu.mycoursestore.product.entity.Product;
import cn.edu.csu.mycoursestore.product.entity.ProductImage;
import cn.edu.csu.mycoursestore.product.entity.ProductStatus;
import cn.edu.csu.mycoursestore.product.mapper.CategoryMapper;
import cn.edu.csu.mycoursestore.product.mapper.ProductImageMapper;
import cn.edu.csu.mycoursestore.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;

    public CatalogService(CategoryMapper categoryMapper,
                          ProductMapper productMapper,
                          ProductImageMapper productImageMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
    }

    public List<CategoryNode> categoryTree() {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));

        Map<Long, List<Category>> childrenByParent = new LinkedHashMap<>();
        for (Category category : categories) {
            if (category.getParentId() != null) {
                childrenByParent.computeIfAbsent(category.getParentId(), key -> new ArrayList<>()).add(category);
            }
        }

        List<CategoryNode> tree = new ArrayList<>();
        for (Category category : categories) {
            if (category.getParentId() == null) {
                List<CategoryNode> children = childrenByParent.getOrDefault(category.getId(), List.of()).stream()
                        .map(child -> new CategoryNode(child.getId(), child.getName(), List.of()))
                        .toList();
                tree.add(new CategoryNode(category.getId(), category.getName(), children));
            }
        }
        return tree;
    }

    public List<ProductCard> featuredProducts(int limit) {
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ProductStatus.ON_SALE)
                .orderByAsc(Product::getSortOrder)
                .orderByDesc(Product::getCreatedAt)
                .last("limit " + Math.clamp(limit, 1, 24)));
        return products.stream()
                .map(product -> new ProductCard(product.getId(), product.getName(), product.getSummary(),
                        product.getCoverImage(), product.getPrice(), stockOf(product)))
                .toList();
    }

    public ProductDetail productDetail(long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() != ProductStatus.ON_SALE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在或已下架");
        }

        List<ProductDetail.GalleryImage> images = productImageMapper.selectList(
                        new LambdaQueryWrapper<ProductImage>()
                                .eq(ProductImage::getProductId, id)
                                .orderByAsc(ProductImage::getSortOrder)
                                .orderByAsc(ProductImage::getId))
                .stream()
                .map(image -> new ProductDetail.GalleryImage(image.getImagePath(), image.getAltText()))
                .toList();

        return new ProductDetail(product.getId(), product.getName(), product.getSummary(),
                product.getDescription(), product.getCoverImage(), product.getPrice(), stockOf(product),
                categoryPath(product.getCategoryId()), images);
    }

    private String categoryPath(Long categoryId) {
        if (categoryId == null) {
            return "";
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            return "";
        }
        if (category.getParentId() == null) {
            return category.getName();
        }
        Category parent = categoryMapper.selectById(category.getParentId());
        return parent == null ? category.getName() : parent.getName() + " / " + category.getName();
    }

    private static int stockOf(Product product) {
        return product.getStock() == null ? 0 : product.getStock();
    }
}