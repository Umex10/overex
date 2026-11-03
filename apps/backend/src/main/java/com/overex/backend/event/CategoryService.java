package com.overex.backend.event;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.overex.backend.common.exception.ApiError;
import com.overex.backend.common.exception.OwnException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Loads the category a payload refers to. An unknown id is a client error
     * (400 on {@code categoryId}), not a missing resource of the URL (404).
     */
    public Category getCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new OwnException(List.of(new ApiError.FieldError("categoryId",
                        String.format("Category '%s' was not found", categoryId)))));
    }

}
