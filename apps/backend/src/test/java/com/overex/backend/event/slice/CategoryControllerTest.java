package com.overex.backend.event.slice;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.overex.backend.event.Category;
import com.overex.backend.event.CategoryController;
import com.overex.backend.event.CategoryService;
import com.overex.backend.event.utils.CategoryTestDataFactory;

@WebMvcTest(CategoryController.class)
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void shouldGetCategories() throws Exception {

        List<Category> categories = CategoryTestDataFactory.createCategoryList();

        when(categoryService.getCategories()).thenReturn(categories);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(categories.size())))
                .andExpect(jsonPath("$[*].id").value(containsInAnyOrder(categories
                        .stream().map(c -> c.getId().toString()).toArray())))
                .andExpect(jsonPath("$[*].title").value(containsInAnyOrder(categories
                        .stream().map(Category::getTitle).toArray())));

    }

}
