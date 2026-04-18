package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.model.Category;

public class CategoryMapper {

    public static CategoryDto toCategoryDto(Category category) {
        if (category == null) return null;
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    public static Category toCategory(ru.practicum.ewm.dto.NewCategoryDto newCategoryDto) {
        if (newCategoryDto == null) return null;
        return Category.builder()
                .name(newCategoryDto.getName())
                .build();
    }
}
