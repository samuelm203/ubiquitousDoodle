package com.example.orders.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-End über REST -> Controller -> Service -> JPA -> H2 (In-Memory-DB, Flyway-Schema). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        "app.demo-data=false"
})
@AutoConfigureMockMvc
class OrderApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void rejectsAnonymousAccess() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAdminMayCreateProducts() throws Exception {
        mvc.perform(post("/api/products").with(httpBasic("user", "user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"X-100\",\"name\":\"X\",\"price\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void placesOrderEndToEnd() throws Exception {
        String customerId = idOf(mvc.perform(post("/api/customers").with(httpBasic("user", "user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test AG\",\"email\":\"test@example.com\",\"city\":\"Bern\"}"))
                .andExpect(status().isCreated()).andReturn());

        String productId = idOf(mvc.perform(post("/api/products").with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"TST-001\",\"name\":\"Testprodukt\",\"price\":19.95}"))
                .andExpect(status().isCreated()).andReturn());

        String orderId = idOf(mvc.perform(post("/api/orders").with(httpBasic("user", "user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\"}"))
                .andExpect(status().isCreated()).andReturn());

        mvc.perform(post("/api/orders/" + orderId + "/place").with(httpBasic("user", "user")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Eine leere Bestellung kann nicht aufgegeben werden"));

        mvc.perform(post("/api/orders/" + orderId + "/lines").with(httpBasic("user", "user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + productId + "\",\"quantity\":3}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/orders/" + orderId + "/place").with(httpBasic("user", "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.total").value(59.85))
                .andExpect(jsonPath("$.lines.length()").value(1));

        mvc.perform(get("/api/orders/" + orderId).with(httpBasic("user", "user")))
                .andExpect(jsonPath("$.customerName").value("Test AG"));
    }

    private static String idOf(MvcResult result) throws Exception {
        Matcher m = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(result.getResponse().getContentAsString());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }
}
