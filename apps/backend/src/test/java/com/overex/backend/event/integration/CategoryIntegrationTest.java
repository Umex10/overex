package com.overex.backend.event.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.overex.backend.auth.utils.AuthTestClient;
import com.overex.backend.auth.utils.TestRsaKeys;
import com.overex.backend.common.repositories.UserRepository;
import com.overex.backend.event.Category;
import com.overex.backend.event.CategoryRepository;
import com.overex.backend.event.utils.CategoryTestDataFactory;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CategoryIntegrationTest {

    /**
     * Temporary folder for the generated key files. JUnit creates it before the
     * Spring context starts and deletes it after all tests of this class.
     */
    @TempDir
    static Path keyDir;

    /**
     * Writes a fresh key pair into {@link #keyDir} and points the key-location
     * properties at it, so the context starts without the git-ignored
     * {@code keys/} folder (e.g. in CI).
     */
    @DynamicPropertySource
    static void keyLocations(DynamicPropertyRegistry registry) {
        TestRsaKeys.writePemFiles(keyDir);
        registry.add("jwt.private-key-location", () -> keyDir.resolve("private.pem").toUri().toString());
        registry.add("jwt.public-key-location", () -> keyDir.resolve("public.pem").toUri().toString());
    }

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

        mockMvc.perform(get("/categories").header("Authorization", "Bearer " + accessTk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(categories.size())))
                .andExpect(jsonPath("$[*].id").value(containsInAnyOrder(categories
                        .stream().map(c -> c.getId().toString()).toArray())))
                .andExpect(jsonPath("$[*].title").value(containsInAnyOrder(categories
                        .stream().map(Category::getTitle).toArray())));
    }

}
