package com.example.urlshortener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testShortenUrl() throws Exception {
        String requestBody = """
            {
              "url": "https://www.example.com"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value(matchesPattern("https?://[^/]+/[A-Za-z0-9]{7}")));
    }

    @Test
    void testShortenUrlWithInvalidInput() throws Exception {
        String requestBody = """
            {
              "url": ""
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRedirectToOriginalUrl() throws Exception {
        // First create a short URL
        String requestBody = """
            {
              "url": "https://www.google.com"
            }
            """;

        MvcResult result = mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andReturn();

        // Extract the code from the response
        String response = result.getResponse().getContentAsString();
        String shortCode = response.split("\"code\":\"")[1].split("\"")[0];

        // Then test the redirect
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://www.google.com"));
    }

    @Test
    void testRedirectWithNonExistentCode() throws Exception {
        mockMvc.perform(get("/INVALID"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testShortenUrlWithCustomCode() throws Exception {
        String requestBody = """
            {
              "url": "https://www.example.com",
              "customCode": "mylink"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value(containsString("/mylink")))
                .andExpect(jsonPath("$.code").value("mylink"));
    }

    @Test
    void testCustomCodeConflict() throws Exception {
        // First, create a short URL with custom code
        String requestBody = """
            {
              "url": "https://www.example.com",
              "customCode": "duplicate"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        // Try to create another URL with the same custom code
        String duplicateRequest = """
            {
              "url": "https://www.another-example.com",
              "customCode": "duplicate"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("already exists")));
    }

    @Test
    void testCustomCodeTooShort() throws Exception {
        String requestBody = """
            {
              "url": "https://www.example.com",
              "customCode": "ab"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("between 3 and 20")));
    }

    @Test
    void testCustomCodeTooLong() throws Exception {
        String requestBody = """
            {
              "url": "https://www.example.com",
              "customCode": "thiscodeiswaytoolongforvalidation"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("between 3 and 20")));
    }

    @Test
    void testCustomCodeWithInvalidCharacters() throws Exception {
        String requestBody = """
            {
              "url": "https://www.example.com",
              "customCode": "my-link"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("alphanumeric")));
    }

    @Test
    void testCustomCodeReservedWord() throws Exception {
        String requestBody = """
            {
              "url": "https://www.example.com",
              "customCode": "api"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("reserved")));
    }

    @Test
    void testRedirectWithCustomCode() throws Exception {
        // Create a short URL with custom code
        String requestBody = """
            {
              "url": "https://www.vibecoding.com",
              "customCode": "vibe"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        // Test the redirect works with custom code
        mockMvc.perform(get("/vibe"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://www.vibecoding.com"));
    }
}
