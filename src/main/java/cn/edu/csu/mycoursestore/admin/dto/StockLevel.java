package cn.edu.csu.mycoursestore.admin.dto;

import lombok.Getter;

@Getter
public enum StockLevel {

    OUT_OF_STOCK("缺货"),
    LOW_STOCK("低库存");

    private final String label;

    StockLevel(String label) {
        this.label = label;
    }
}
