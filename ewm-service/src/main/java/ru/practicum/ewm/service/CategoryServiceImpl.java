package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.error.NotFoundException;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);
        return categoryRepository.findAll(pageable).stream()
                .map(CategoryMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не найдена"));
        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Creating category: {}", newCategoryDto);
        Category category = CategoryMapper.toCategory(newCategoryDto);
        category = categoryRepository.save(category);
        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        log.info("Updating category with id: {}", catId);
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не найдена"));
        category.setName(categoryDto.getName());
        category = categoryRepository.save(category);
        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Удаление категории с id: {}", catId);
        categoryRepository.deleteById(catId);
    }
}