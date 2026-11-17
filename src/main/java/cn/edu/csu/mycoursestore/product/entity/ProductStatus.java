package cn.edu.csu.mycoursestore.product.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ProductStatus {

    DRAFT("DRAFT"),
    ON_SALE("ON_SALE");

    @EnumValue
    private final String value;

    ProductStatus(String value) {
        this.value = value;
    }
}
