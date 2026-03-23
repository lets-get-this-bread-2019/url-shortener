package com.example.urlshortener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.example.urlshortener.TestHelper.uniqueTestIp;
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
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
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
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRedirectToOriginalUrl() throws Exception {
        String testIp = uniqueTestIp();
        // First create a short URL
        String requestBody = """
            {
              "url": "https://www.google.com"
            }
            """;

        MvcResult result = mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andReturn();

        // Extract the code from the response
        String response = result.getResponse().getContentAsString();
        String shortCode = response.split("\"code\":\"")[1].split("\"")[0];

        // Then test the redirect
        mockMvc.perform(get("/" + shortCode)
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://www.google.com"));
    }

    @Test
    void testRedirectWithNonExistentCode() throws Exception {
        mockMvc.perform(get("/INVALID")
                .header("X-Forwarded-For", uniqueTestIp()))
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
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value(containsString("/mylink")))
                .andExpect(jsonPath("$.code").value("mylink"));
    }

    @Test
    void testCustomCodeConflict() throws Exception {
        String testIp = uniqueTestIp();
        // First, create a short URL with custom code
        String requestBody = """
            {
              "url": "https://www.example.com",
              "customCode": "duplicate"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", testIp))
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
                .content(duplicateRequest)
                .header("X-Forwarded-For", testIp))
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
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
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
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
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
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
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
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("reserved")));
    }

    @Test
    void testRedirectWithCustomCode() throws Exception {
        String testIp = uniqueTestIp();
        // Create a short URL with custom code
        String requestBody = """
            {
              "url": "https://www.vibecoding.com",
              "customCode": "vibe"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk());

        // Test the redirect works with custom code
        mockMvc.perform(get("/vibe")
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://www.vibecoding.com"));
    }

    // Security tests for URL validation

    @Test
    void testShortenUrlWithHttpScheme() throws Exception {
        String requestBody = """
            {
              "url": "http://www.example.com"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void testRejectJavascriptScheme() throws Exception {
        String requestBody = """
            {
              "url": "javascript:alert('XSS')"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Only http and https URLs are allowed")));
    }

    @Test
    void testRejectDataScheme() throws Exception {
        String requestBody = """
            {
              "url": "data:text/html,<script>alert('XSS')</script>"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Invalid URL format")));
    }

    @Test
    void testRejectFileScheme() throws Exception {
        String requestBody = """
            {
              "url": "file:///etc/passwd"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Only http and https URLs are allowed")));
    }

    @Test
    void testRejectFtpScheme() throws Exception {
        String requestBody = """
            {
              "url": "ftp://ftp.example.com/file.txt"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Only http and https URLs are allowed")));
    }

    @Test
    void testRejectUrlWithoutScheme() throws Exception {
        String requestBody = """
            {
              "url": "www.example.com"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("must include a scheme")));
    }

    @Test
    void testRejectMalformedUrl() throws Exception {
        String requestBody = """
            {
              "url": "ht!tp://invalid url with spaces"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testRejectUrlWithoutHost() throws Exception {
        String requestBody = """
            {
              "url": "https://"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Invalid URL format")));
    }

    @Test
    void testRejectUrlSchemeCaseInsensitive() throws Exception {
        String requestBody = """
            {
              "url": "JAVASCRIPT:alert('XSS')"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Only http and https URLs are allowed")));
    }

    // Expiration tests

    @Test
    void testShortenUrlWithTtl() throws Exception {
        String requestBody = """
            {
              "url": "https://www.example.com",
              "ttlSeconds": 3600
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.shortUrl").exists());
    }

    @Test
    void testShortenUrlWithoutTtlNeverExpires() throws Exception {
        String testIp = uniqueTestIp();
        String requestBody = """
            {
              "url": "https://www.example.com"
            }
            """;

        MvcResult result = mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String shortCode = response.split("\"code\":\"")[1].split("\"")[0];

        // Verify the redirect works (no expiration)
        mockMvc.perform(get("/" + shortCode)
                .header("X-Forwarded-For", testIp))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://www.example.com"));
    }

    @Test
    void testShortenUrlWithNegativeTtlRejects() throws Exception {
        String requestBody = """
            {
              "url": "https://www.example.com",
              "ttlSeconds": -100
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("must be positive")));
    }

    @Test
    void testShortenUrlWithZeroTtlRejects() throws Exception {
        String requestBody = """
            {
              "url": "https://www.example.com",
              "ttlSeconds": 0
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("must be positive")));
    }

    @Test
    void testShortenUrlWithInvalidTtlTypeRejects() throws Exception {
        String requestBody = """
            {
              "url": "https://www.example.com",
              "ttlSeconds": "not-a-number"
            }
            """;

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Forwarded-For", uniqueTestIp()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("must be a number")));
    }
}
