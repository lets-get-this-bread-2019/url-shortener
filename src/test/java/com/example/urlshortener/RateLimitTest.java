package com.example.urlshortener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testShortenEndpointRateLimit() throws Exception {
        String testIp = "192.168.1.100";
        String requestBody = "{\"url\":\"https://example.com\"}";

        // First 10 requests should succeed (default limit is 10/minute)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk());
        }

        // 11th request should be rate limited
        MvcResult result = mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", testIp))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(content().json("{\"error\":\"Too many requests. Please try again later.\"}"))
            .andReturn();

        // Verify Retry-After header is present and is a positive integer
        String retryAfter = result.getResponse().getHeader("Retry-After");
        assert retryAfter != null;
        assert Integer.parseInt(retryAfter) >= 0;
    }

    @Test
    void testRedirectEndpointRateLimit() throws Exception {
        String testIp = "192.168.1.101";

        // First, create a short URL
        String requestBody = "{\"url\":\"https://example.com\"}";
        MvcResult createResult = mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", "192.168.1.200")) // Different IP to avoid shorten rate limit
            .andExpect(status().isOk())
            .andReturn();

        // Extract the code from the response
        String response = createResult.getResponse().getContentAsString();
        String code = response.split("\"code\":\"")[1].split("\"")[0];

        // First 100 requests should succeed (default limit is 100/minute)
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/" + code)
                    .header("X-Forwarded-For", testIp))
                .andExpect(status().isFound()); // 302 redirect
        }

        // 101st request should be rate limited
        MvcResult result = mockMvc.perform(get("/" + code)
                .header("X-Forwarded-For", testIp))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(content().json("{\"error\":\"Too many requests. Please try again later.\"}"))
            .andReturn();

        // Verify Retry-After header
        String retryAfter = result.getResponse().getHeader("Retry-After");
        assert retryAfter != null;
        assert Integer.parseInt(retryAfter) >= 0;
    }

    @Test
    void testDifferentIpAddressesHaveIndependentLimits() throws Exception {
        String requestBody = "{\"url\":\"https://example.com\"}";

        // Make 10 requests from IP1
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("X-Forwarded-For", "192.168.1.102"))
                .andExpect(status().isOk());
        }

        // IP1 should be rate limited
        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", "192.168.1.102"))
            .andExpect(status().isTooManyRequests());

        // IP2 should still be able to make requests
        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", "192.168.1.103"))
            .andExpect(status().isOk());
    }

    @Test
    void testNonRateLimitedEndpointsNotAffected() throws Exception {
        String testIp = "192.168.1.104";

        // Home endpoint should not be rate limited
        // (making many requests to verify it doesn't trigger rate limiting)
        for (int i = 0; i < 150; i++) {
            mockMvc.perform(get("/")
                    .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk());
        }
    }
}
