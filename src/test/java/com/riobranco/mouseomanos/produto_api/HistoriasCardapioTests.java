package com.riobranco.mouseomanos.produto_api;

import com.riobranco.mouseomanos.produto_api.repository.PedidoRepository;
import com.riobranco.mouseomanos.produto_api.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:historias-tests", "spring.jpa.hibernate.ddl-auto=create-drop"
})
class HistoriasCardapioTests {
    @LocalServerPort private int port;
    @Autowired private ProdutoRepository produtos;
    @Autowired private PedidoRepository pedidos;
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void limparBancoDeTeste() { pedidos.deleteAll(); produtos.deleteAll(); }

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode get(String path) throws Exception {
        var response = request("GET", path, null);
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private long produto(String nome, String valor, boolean disponivel) throws Exception {
        var response = request("POST", "/produtos", """
                {"nome":"%s","categoria":"Hambúrgueres","descricao":"Feito na casa","valor":%s,"disponivel":%s}
                """.formatted(nome, valor, disponivel));
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body()).get("id").asLong();
    }

    private HttpResponse<String> pedir(String itens) throws Exception {
        return request("POST", "/pedidos", "{\"cliente\":\"Mesa 4\",\"observacao\":\"Sem cebola\",\"itens\":" + itens + "}");
    }

    @Test
    void historia1VisualizaCardapioPreviamenteCadastrado() throws Exception {
        long id = produto("X-Bacon", "12.50", true);
        produto("X-Duplo esgotado", "10.00", false);
        var menu = get("/cardapio");
        assertEquals(1, menu.size());
        assertEquals(id, menu.get(0).get("id").asLong());
        assertEquals("X-Bacon", menu.get(0).get("nome").asString());
        assertEquals(12.5, menu.get(0).get("valor").asDouble());
        assertEquals(2, get("/produtos").size());
    }

    @Test
    void historia2AlteraPrecoERefleteNoCardapio() throws Exception {
        long id = produto("X-Clássico", "12.50", true);
        assertEquals(200, request("PATCH", "/produtos/" + id + "/preco", "{\"valor\":15.90}").statusCode());
        assertEquals(0, produtos.findById(id).orElseThrow().getValor().compareTo(new java.math.BigDecimal("15.90")));
        assertEquals(15.90, get("/cardapio").get(0).get("valor").asDouble());
        assertEquals("X-Clássico", get("/produtos/" + id).get("nome").asString());
    }

    @Test
    void historia3NovosItensAparecemEExcluidosSomem() throws Exception {
        assertEquals(0, get("/cardapio").size());
        long id = produto("X-Squad Especial", "8.00", true);
        assertTrue(produtos.existsById(id));
        assertEquals(1, get("/cardapio").size());
        assertEquals(200, request("DELETE", "/produtos/" + id, null).statusCode());
        assertFalse(produtos.existsById(id));
        assertEquals(0, get("/cardapio").size());
        assertEquals(404, request("GET", "/produtos/" + id, null).statusCode());
    }

    @Test
    void disponibilidadeRetiraERetornaItemAoCardapio() throws Exception {
        long id = produto("X-Clássico", "12.00", true);
        assertEquals(200, request("PATCH", "/produtos/" + id + "/disponibilidade", "{\"disponivel\":false}").statusCode());
        assertEquals(0, get("/cardapio").size());
        assertEquals(1, get("/produtos").size());
        assertEquals(200, request("PATCH", "/produtos/" + id + "/disponibilidade", "{\"disponivel\":true}").statusCode());
        assertEquals(1, get("/cardapio").size());
    }

    @Test
    void historia4CriaEConsultaPedidoComTotalExato() throws Exception {
        long a = produto("Item A", "0.10", true), b = produto("Item B", "0.20", true);
        var response = pedir("[{\"produtoId\":" + a + ",\"quantidade\":2},{\"produtoId\":" + b + ",\"quantidade\":3}]");
        assertEquals(201, response.statusCode(), response.body());
        var created = json.readTree(response.body());
        assertEquals(0.80, created.get("total").asDouble(), 0.00001);
        assertEquals(2, created.get("itens").size());
        assertEquals("CONFIRMADO", created.get("status").asString());
        assertEquals("Mesa 4", created.get("cliente").asString());
        assertEquals("Sem cebola", created.get("observacao").asString());
        assertFalse(created.get("criadoEm").asString().isBlank());
        assertEquals(created, get("/pedidos/" + created.get("id").asLong()));
        assertEquals(1, get("/pedidos").size());
    }

    @Test
    void historia4RecusaPedidoInteiroQuandoUmItemEstaIndisponivel() throws Exception {
        long a = produto("Disponível", "12.00", true), b = produto("Esgotado", "10.00", false);
        var response = pedir("[{\"produtoId\":" + a + ",\"quantidade\":1},{\"produtoId\":" + b + ",\"quantidade\":1}]");
        assertEquals(409, response.statusCode());
        assertTrue(response.body().contains("disponível"));
        assertEquals(0, pedidos.count());
    }

    @Test
    void historia4RecusaItemExcluidoDepoisDeAdicionadoAoCarrinho() throws Exception {
        long id = produto("X-Clássico", "12.00", true);
        request("DELETE", "/produtos/" + id, null);
        assertEquals(409, pedir("[{\"produtoId\":" + id + ",\"quantidade\":1}]").statusCode());
        assertEquals(0, pedidos.count());
    }

    @Test
    void historia4ExigeRevisaoSePrecoMudouAntesDeConfirmar() throws Exception {
        long id = produto("X-Clássico", "12.00", true);
        request("PATCH", "/produtos/" + id + "/preco", "{\"valor\":15.00}");
        assertEquals(409, pedir("[{\"produtoId\":" + id + ",\"quantidade\":1,\"precoUnitario\":12.00}]").statusCode());
        assertEquals(0, pedidos.count());
        var accepted = pedir("[{\"produtoId\":" + id + ",\"quantidade\":1,\"precoUnitario\":15.00}]");
        assertEquals(201, accepted.statusCode());
        assertEquals(15.0, json.readTree(accepted.body()).get("total").asDouble());
    }

    @Test
    void pedidosPreservamNomeEPrecoMesmoDepoisDeEditarEExcluirItem() throws Exception {
        long id = produto("X-Clássico original", "12.50", true);
        var created = json.readTree(pedir("[{\"produtoId\":" + id + ",\"quantidade\":2}]").body());
        assertEquals(200, request("PUT", "/produtos/" + id, """
                {"nome":"Outro nome","categoria":"Doces","descricao":"Nova receita","valor":99.90,"disponivel":true}
                """).statusCode());
        request("DELETE", "/produtos/" + id, null);
        var saved = get("/pedidos/" + created.get("id").asLong());
        assertEquals("X-Clássico original", saved.get("itens").get(0).get("nome").asString());
        assertEquals(12.50, saved.get("itens").get(0).get("precoUnitario").asDouble());
        assertEquals(25.0, saved.get("total").asDouble());
        assertEquals(0, get("/cardapio").size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "100", "1.5", "null"})
    void recusaQuantidadesInvalidas(String quantidade) throws Exception {
        long id = produto("X-Clássico", "12.00", true);
        assertEquals(400, pedir("[{\"produtoId\":" + id + ",\"quantidade\":" + quantidade + "}]").statusCode());
        assertEquals(0, pedidos.count());
    }

    @Test
    void recusaPedidoVazioDuplicadoOuSemCliente() throws Exception {
        assertEquals(400, pedir("[]").statusCode());
        long id = produto("X-Clássico", "12.00", true);
        String item = "{\"produtoId\":" + id + ",\"quantidade\":1}";
        assertEquals(400, pedir("[" + item + "," + item + "]").statusCode());
        assertEquals(400, request("POST", "/pedidos", "{\"cliente\":\"  \",\"itens\":[" + item + "]}").statusCode());
        assertEquals(404, request("GET", "/pedidos/999999", null).statusCode());
        assertEquals(0, pedidos.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0.001", "null", "10000000000"})
    void recusaPrecosInvalidosSemAlterarCardapio(String valor) throws Exception {
        long id = produto("X-Clássico", "12.00", true);
        assertEquals(400, request("PATCH", "/produtos/" + id + "/preco", "{\"valor\":" + valor + "}").statusCode());
        assertEquals(12.0, get("/cardapio").get(0).get("valor").asDouble());
    }
}
