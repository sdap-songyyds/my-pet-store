package cn.edu.csu.mycoursestore.admin.dto;

public record AdminInventoryFilter(String keyword, Long categoryId, StockLevel level) {
}
