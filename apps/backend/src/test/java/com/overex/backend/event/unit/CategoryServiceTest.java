package com.overex.backend.event.unit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.overex.backend.common.exception.OwnException;
import com.overex.backend.event.Category;
import com.overex.backend.event.CategoryRepository;
import com.overex.backend.event.CategoryService;
import com.overex.backend.event.utils.CategoryTestDataFactory;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    CategoryService categoryService;

    @Test
    void shouldGetCategories() {

        // Arrange
        List<Category> categories = CategoryTestDataFactory.createCategoryList();

        when(categoryRepository.findAll()).thenReturn(categories);

        // Act
        List<Category> foundCategories = categoryService.getCategories();

        // Assert
        assertSame(categories, foundCategories);

    }

    @Test
    void shouldGetCategory() {

        // Arrange
        Category category = CategoryTestDataFactory.createDefaultCategory();

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        // Act
        Category foundCategory = categoryService.getCategory(category.getId());

        // Assert
        assertSame(category, foundCategory);
    }

    @Test
    void shouldNotGetCategory() {

        // Arrange
        when(categoryRepository.findById(any())).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(OwnException.class,
                () -> categoryService.getCategory(any()));
    }

}
