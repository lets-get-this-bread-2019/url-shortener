package com.example.urlshortener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.example.urlshortener.TestHelper.uniqueTestIp;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testClickTrackingAndAnalytics() throws Exception {
        String testIp = uniqueTestIp();

        // Create a short URL
        String requestBody = """
            {
              "url": "https://www.example.com/analytics-test"
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        String shortCode = response.split("\"code\":\"")[1].split("\"")[0];

        // Perform multiple clicks from different IPs
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/" + shortCode)
                    .header("X-Forwarded-For", uniqueTestIp())
                    .header("User-Agent", "TestBot/1.0")
                    .header("Referer", "https://www.referrer.com"))
                    .andExpect(status().isFound());
        }

        // Perform a click from the same IP again
        mockMvc.perform(get("/" + shortCode)
                .header("X-Forwarded-For", testIp)
                .header("User-Agent", "TestBot/1.0")
                .header("Referer", "https://www.referrer.com"))
                .andExpect(status().isFound());

        // Get analytics
        mockMvc.perform(get("/analytics/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(shortCode))
                .andExpect(jsonPath("$.originalUrl").value("https://www.example.com/analytics-test"))
                .andExpect(jsonPath("$.totalClicks").value(4))
                .andExpect(jsonPath("$.uniqueIps").value(4)) // 3 unique + 1 from creation IP
                .andExpect(jsonPath("$.clicksByDate").isMap())
                .andExpect(jsonPath("$.topReferrers['https://www.referrer.com']").value(4));
    }

    @Test
    void testAnalyticsWithNonExistentCode() throws Exception {
        mockMvc.perform(get("/analytics/INVALID"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAnalyticsWithNoClicks() throws Exception {
        String testIp = uniqueTestIp();

        // Create a short URL but don't click it
        String requestBody = """
            {
              "url": "https://www.example.com/no-clicks"
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        String shortCode = response.split("\"code\":\"")[1].split("\"")[0];

        // Get analytics (should show 0 clicks)
        mockMvc.perform(get("/analytics/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(shortCode))
                .andExpect(jsonPath("$.totalClicks").value(0))
                .andExpect(jsonPath("$.uniqueIps").value(0));
    }

    @Test
    void testPrivacyPreservation() throws Exception {
        String testIp = uniqueTestIp();

        // Create a short URL
        String requestBody = """
            {
              "url": "https://www.example.com/privacy-test"
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        String shortCode = response.split("\"code\":\"")[1].split("\"")[0];

        // Click the URL
        mockMvc.perform(get("/" + shortCode)
                .header("X-Forwarded-For", testIp)
                .header("User-Agent", "PrivacyBot/1.0"))
                .andExpect(status().isFound());

        // Get analytics - should NOT contain raw IP address in response
        MvcResult analyticsResult = mockMvc.perform(get("/analytics/" + shortCode))
                .andExpect(status().isOk())
                .andReturn();

        String analyticsResponse = analyticsResult.getResponse().getContentAsString();
        // Verify the response doesn't contain the raw IP
        assert !analyticsResponse.contains(testIp) :
            "Analytics response should not contain raw IP address";
    }

    @Test
    void testMultipleReferrers() throws Exception {
        String testIp = uniqueTestIp();

        // Create a short URL
        String requestBody = """
            {
              "url": "https://www.example.com/referrer-test"
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        String shortCode = response.split("\"code\":\"")[1].split("\"")[0];

        // Click from different referrers
        mockMvc.perform(get("/" + shortCode)
                .header("X-Forwarded-For", uniqueTestIp())
                .header("Referer", "https://google.com"))
                .andExpect(status().isFound());

        mockMvc.perform(get("/" + shortCode)
                .header("X-Forwarded-For", uniqueTestIp())
                .header("Referer", "https://google.com"))
                .andExpect(status().isFound());

        mockMvc.perform(get("/" + shortCode)
                .header("X-Forwarded-For", uniqueTestIp())
                .header("Referer", "https://twitter.com"))
                .andExpect(status().isFound());

        // Get analytics
        mockMvc.perform(get("/analytics/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(3))
                .andExpect(jsonPath("$.topReferrers['https://google.com']").value(2))
                .andExpect(jsonPath("$.topReferrers['https://twitter.com']").value(1));
    }
}
