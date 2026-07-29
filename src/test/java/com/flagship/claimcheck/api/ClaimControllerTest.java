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
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.fields.claimId").exists()).andExpect(jsonPath("$.fields.amount").exists());
    }
}
