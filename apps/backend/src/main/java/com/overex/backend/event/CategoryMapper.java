package com.overex.backend.event;

import com.overex.backend.event.dto.responses.CategoryResponseDto;

public interface CategoryMapper {

    CategoryResponseDto toCategoryResponseDto(Category category);
    
}
