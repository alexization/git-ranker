package com.gitranker.api.docs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("openapi")
class OpenApiDocsTest {

    private static final String OPENAPI_OUTPUT_PROPERTY = "openapi.output";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("OpenAPI JSON은 공개 /api/v1 계약과 보안 스키마를 노출한다")
    void shouldExposeOpenApiJsonForPublicApi() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.paths['/api/v1/ranking'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/{username}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/{username}/refresh'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/me'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/refresh'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/logout/all'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/badges/{nodeId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/badges/{tier}/badge'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.accessTokenCookie").exists())
                .andExpect(jsonPath("$.components.securitySchemes.refreshTokenCookie").exists())
                .andExpect(jsonPath("$.servers[0].url").value("https://www.git-ranker.com"))
                .andExpect(jsonPath("$.servers[1].url").value("http://localhost:8080"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("\"/actuator/health\"");

        persistIfRequested(responseBody);
    }

    @Test
    @DisplayName("Swagger UI는 브라우저 경로에서 노출된다")
    void shouldExposeSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("swagger-ui")));
    }

    private void persistIfRequested(String responseBody) throws IOException {
        String outputPath = System.getProperty(OPENAPI_OUTPUT_PROPERTY);
        if (outputPath == null || outputPath.isBlank()) {
            return;
        }

        Path outputFile = Path.of(outputPath);
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputFile, responseBody);
    }
}
