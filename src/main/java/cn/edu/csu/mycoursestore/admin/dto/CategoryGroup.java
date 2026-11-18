package cn.edu.csu.mycoursestore.admin.dto;

import java.util.List;

public record CategoryGroup(String name, List<CategoryOption> children) {
}
