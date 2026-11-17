package cn.edu.csu.mycoursestore.common;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FormErrors {

    private FormErrors() {
    }

    public static void add(Model model, String prefix, BindingResult binding) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : binding.getFieldErrors()) {
            fieldErrors.computeIfAbsent(error.getField(), field -> new ArrayList<>())
                    .add(error.getDefaultMessage());
        }
        List<String> globalErrors = binding.getGlobalErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .toList();
        model.addAttribute(prefix + "FieldErrors", fieldErrors);
        model.addAttribute(prefix + "GlobalErrors", globalErrors);
    }
}