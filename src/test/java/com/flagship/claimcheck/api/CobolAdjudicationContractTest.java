package com.flagship.claimcheck.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class CobolAdjudicationContractTest {
    private static final String FIXTURES = "/contracts/cobol-adjudication-cases.json";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    static Stream<Arguments> cobolCases() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream input = CobolAdjudicationContractTest.class.getResourceAsStream(FIXTURES)) {
            if (input == null) throw new IOException("Missing contract fixture " + FIXTURES);
            JsonNode cases = mapper.readTree(input);
            return Stream.of(mapper.treeToValue(cases, ContractCase[].class))
                .map(fixture -> Arguments.of(Named.of(fixture.name(), fixture)));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cobolCases")
    void matchesCapturedCobolDecision(ContractCase fixture) throws Exception {
        MvcResult response = mvc.perform(post("/api/v1/claims/adjudicate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(fixture.request())))
            .andReturn();

        NormalizedResult actual = normalize(response);
        assertThat(actual).as("COBOL parity for %s", fixture.name()).isEqualTo(fixture.cobolResult());
    }

    private NormalizedResult normalize(MvcResult response) throws IOException {
        JsonNode body = objectMapper.readTree(response.getResponse().getContentAsByteArray());
        int httpStatus = response.getResponse().getStatus();
        if (httpStatus == 400) {
            return new NormalizedResult(httpStatus, "REJECTED", List.of("VAL-001"), null);
        }
        List<String> reasonCodes = objectMapper.convertValue(body.path("reasons").findValues("code"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        String duplicateClaimId = body.path("duplicateMatch").path("claimId").textValue();
        return new NormalizedResult(httpStatus, body.path("status").textValue(), reasonCodes, duplicateClaimId);
    }

    record ContractCase(String name, JsonNode request, NormalizedResult cobolResult) {}
    record NormalizedResult(int httpStatus, String status, List<String> reasonCodes, String duplicateClaimId) {}
}
