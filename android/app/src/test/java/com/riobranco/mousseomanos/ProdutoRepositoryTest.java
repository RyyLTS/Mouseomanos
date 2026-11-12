package com.riobranco.mousseomanos;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import okhttp3.mockwebserver.*;
import org.junit.*;
import static org.junit.Assert.*;

public class ProdutoRepositoryTest {
    private MockWebServer server;
    private ProdutoRepository repo;
    @Before public void preparar() throws Exception {
        server = new MockWebServer(); server.start();
        repo = new ProdutoRepository(RetrofitClient.criar(server.url("/").toString()));
    }
    @After public void fechar() throws Exception { repo.cancelar(); server.shutdown(); }
    private static class Resultado<T> implements ProdutoRepository.Retorno<T> {
        final CountDownLatch pronto = new CountDownLatch(1); T dado; String erro;
        public void sucesso(T dado) { this.dado = dado; pronto.countDown(); }
        public void erro(String erro) { this.erro = erro; pronto.countDown(); }
        void aguardar() throws Exception { assertTrue(pronto.await(5, TimeUnit.SECONDS)); }
    }
    @Test public void listagemConverteJsonReal() throws Exception {
        server.enqueue(new MockResponse().setBody("[{\"id\":7,\"nome\":\"Mouse\",\"categoria\":\"Periférico\",\"descricao\":\"Sem fio\",\"valor\":19.90,\"disponivel\":true}]"));
        Resultado<List<Produto>> r = new Resultado<>(); repo.listar(r); r.aguardar();
        assertNull(r.erro); assertEquals(new BigDecimal("19.90"), r.dado.get(0).valor);
        assertEquals("/produtos", server.takeRequest().getPath());
    }
    @Test public void cadastroEnviaJsonSemIdEAceitaCorpoVazio() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));
        Resultado<Void> r = new Resultado<>(); repo.criar(new Produto("Mouse", "Periférico", "Sem fio", BigDecimal.TEN, false), r); r.aguardar();
        RecordedRequest req = server.takeRequest(); assertEquals("POST", req.getMethod());
        String body = req.getBody().readUtf8(); assertFalse(body.contains("\"id\""));
        assertTrue(body.contains("\"disponivel\":false")); assertNull(r.erro);
    }
    @Test public void exclusaoUsaMetodoECaminhoCorretos() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));
        Resultado<Void> r = new Resultado<>(); repo.excluir(7L, r); r.aguardar();
        RecordedRequest req = server.takeRequest(); assertEquals("DELETE", req.getMethod()); assertEquals("/produtos/7", req.getPath()); assertNull(r.erro);
    }
    @Test public void erroHttpPreservaMensagemDoServidor() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(409).setBody("{\"message\":\"O preço mudou\"}"));
        Resultado<List<Produto>> r = new Resultado<>(); repo.listar(r); r.aguardar();
        assertTrue(r.erro.contains("409")); assertTrue(r.erro.contains("O preço mudou"));
    }
}
