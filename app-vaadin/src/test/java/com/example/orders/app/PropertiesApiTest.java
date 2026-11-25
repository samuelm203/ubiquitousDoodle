package com.example.orders.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** REST -> Controller -> Service -> Dateien: 5 Demo-Firmen in einem temporären Ordner. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:propstest;DB_CLOSE_DELAY=-1",
        "app.demo-data=false",
        "app.properties.demo-data=true"
})
@AutoConfigureMockMvc
class PropertiesApiTest {

    private static final Path ROOT = createTempRoot();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.properties.root", ROOT::toString);
    }

    private static Path createTempRoot() {
        try {
            return Files.createTempDirectory("orders-properties-test");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void onlyAdminsMayAccess() throws Exception {
        mvc.perform(get("/api/properties/companies")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/properties/companies").with(httpBasic("user", "user"))).andExpect(status().isForbidden());
        mvc.perform(put("/api/properties/seeland-bau").with(httpBasic("user", "user"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"changes\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsFiveCompaniesWithFourColumns() throws Exception {
        mvc.perform(get("/api/properties/companies").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].columns", hasSize(4)))
                .andExpect(jsonPath("$[0].keyCount", greaterThan(990)));
    }

    @Test
    void returnsTablePagedAndFiltered() throws Exception {
        mvc.perform(get("/api/properties/seeland-bau?size=50&page=1").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns", hasSize(4)))
                .andExpect(jsonPath("$.rows", hasSize(50)))
                .andExpect(jsonPath("$.rows[0].values", hasSize(4)))
                .andExpect(jsonPath("$.total", greaterThan(990)));

        mvc.perform(get("/api/properties/seeland-bau?filter=service.s042.url").with(httpBasic("admin", "admin")))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].key").value("service.s042.url"));
    }

    @Test
    void editsAValueAndWritesTheFile() throws Exception {
        mvc.perform(put("/api/properties/mueller-partner").with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"changes":[{"column":"prod","key":"service.s001.timeout-ms","value":"4242"}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(1));

        String file = Files.readString(ROOT.resolve("mueller-partner/prod.properties"), StandardCharsets.UTF_8);
        assertThat(file).contains("service.s001.timeout-ms=4242").startsWith("# Müller & Partner - Umgebung prod");
    }

    @Test
    void rejectsInvalidInputWithProblemDetails() throws Exception {
        mvc.perform(put("/api/properties/seeland-bau").with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changes\":[{\"column\":\"qa\",\"key\":\"a\",\"value\":\"b\"}]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Unbekannte Spalte")));

        mvc.perform(get("/api/properties/gibt-es-nicht").with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }
}
