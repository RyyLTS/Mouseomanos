package com.riobranco.mouseomanos.produto_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:original-tests", "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProdutoApiApplicationTests {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final JsonMapper json = JsonMapper.builder().build();

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

	@Test
	void contextLoads() {
	}

    @Test
    void serveInterfaceEArquivosEstaticos() throws Exception {
        var page = request("GET", "/", null);
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("X-Squad · Hamburgueria"));
        assertTrue(page.body().contains("id=\"product-form\""));
        assertEquals(200, request("GET", "/app.js", null).statusCode());
        assertEquals(200, request("GET", "/style.css", null).statusCode());
    }

    @Test
    void cadastraConsultaAtualizaEExcluiProduto() throws Exception {
        var created = request("POST", "/produtos", """
                {"nome":"X-Burger de teste","categoria":"Hambúrgueres","descricao":"Teste HTTP","valor":99.90}
                """);
        assertEquals(200, created.statusCode());
        long id = json.readTree(created.body()).get("id").asLong();
        try {
            var found = request("GET", "/produtos/" + id, null);
            assertEquals(200, found.statusCode());
            assertEquals("X-Burger de teste", json.readTree(found.body()).get("nome").asString());
            var listed = request("GET", "/produtos", null);
            assertEquals(200, listed.statusCode());
            assertTrue(json.readTree(listed.body()).valueStream().anyMatch(item -> item.get("id").asLong() == id));

            var updated = request("PUT", "/produtos/" + id, """
                    {"nome":"X-Burger atualizado","categoria":"Combos","descricao":"Descrição atualizada","valor":149.50}
                    """);
            assertEquals(200, updated.statusCode());
            var saved = json.readTree(request("GET", "/produtos/" + id, null).body());
            assertEquals("X-Burger atualizado", saved.get("nome").asString());
            assertEquals("Combos", saved.get("categoria").asString());
            assertEquals("Descrição atualizada", saved.get("descricao").asString());
            assertEquals(149.50, saved.get("valor").asDouble(), 0.001);
        } finally {
            assertEquals(200, request("DELETE", "/produtos/" + id, null).statusCode());
        }
        var remaining = json.readTree(request("GET", "/produtos", null).body());
        assertTrue(remaining.valueStream().noneMatch(item -> item.get("id").asLong() == id));
    }

}
