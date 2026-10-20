package com.overex.backend.event.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.overex.backend.auth.utils.AuthTestClient;
import com.overex.backend.common.repositories.UserRepository;
import com.overex.backend.event.Category;
import com.overex.backend.event.CategoryRepository;
import com.overex.backend.event.utils.CategoryTestDataFactory;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    ObjectMapper objectMapper;

    private List<Category> categories = new ArrayList<>();

    @BeforeEach
    void setUpDatabase() {
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        for (Category category : CategoryTestDataFactory.createUnsavedCategoryList()) {
            categories.add(categoryRepository.save(category));
        }
    }

    @Test
    void shouldGetCategories() throws Exception {

        String accessTk = AuthTestClient.signUpAndReturnAccessTk(mockMvc, objectMapper);

        mockMvc.perform(get("/category").header("Authorization", "Bearer " + accessTk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(categories.size())))
                .andExpect(jsonPath("$[*].id").value(containsInAnyOrder(categories
                        .stream().map(c -> c.getId().toString()).toArray())))
                .andExpect(jsonPath("$[*].title").value(containsInAnyOrder(categories
                        .stream().map(Category::getTitle).toArray())));
    }

}
