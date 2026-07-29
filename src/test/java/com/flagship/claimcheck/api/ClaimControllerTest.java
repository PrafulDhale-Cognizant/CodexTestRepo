package com.flagship.claimcheck.api;

import com.flagship.claimcheck.service.AdjudicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClaimController.class)
class ClaimControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AdjudicationService service;

    @Test void reportsStructuredValidationErrors() throws Exception {
        mvc.perform(post("/api/v1/claims/adjudicate").contentType("application/json")
            .content("""{"claimId":"bad","memberId":"","providerId":"","procedureCode":"","serviceDate":"2099-01-01","amount":0}"""))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.title").value("Request validation failed"))
            .andExpect(jsonPath("$.errors.claimId").exists()).andExpect(jsonPath("$.errors.amount").exists())
            .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test void reportsMalformedJsonAsAProblem() throws Exception {
        mvc.perform(post("/api/v1/claims/adjudicate").contentType("application/json").content("{bad"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.type").value("https://api.claimcheck.example/problems/malformed-request"));
    }
}
