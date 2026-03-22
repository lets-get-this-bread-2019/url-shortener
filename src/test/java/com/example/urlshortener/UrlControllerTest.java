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
                .andExpect(jsonPath("$.shortUrl").value(matchesPattern("http://localhost:8080/[A-Za-z0-9]{7}")));
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
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String shortCode = response.split("/")[3].replace("\"}", "");

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
}
