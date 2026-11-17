package cn.edu.csu.mycoursestore.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sku;

    private Long categoryId;

    private String name;

    private String summary;

    private String description;

    private String coverImage;

    private BigDecimal price;

    private Integer stock;

    private ProductStatus status;

    private Integer sortOrder;

    private Long version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
