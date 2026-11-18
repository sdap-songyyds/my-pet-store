package cn.edu.csu.mycoursestore.admin.dto;

import java.util.List;

public record AdminCategoryGroup(AdminCategoryRow parent, List<AdminCategoryRow> children) {
}
