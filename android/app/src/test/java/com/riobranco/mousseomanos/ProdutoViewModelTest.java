package com.riobranco.mousseomanos;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import org.junit.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.Assert.*;

public class ProdutoViewModelTest {
    @Rule public InstantTaskExecutorRule executor = new InstantTaskExecutorRule();
    private FakeRepository repo;
    private ProdutoViewModel vm;
    @Before public void preparar() {
        repo = new FakeRepository(); vm = new ProdutoViewModel(repo);
        vm.configurar("https://example.test/");
    }
    @Test public void nomeVazioNaoEnviaRequisicao() {
        vm.salvar(null, " ", "Mouse", "Sem fio", "10", true);
        assertEquals(0, repo.gravacoes);
    }
    @Test public void valorInvalidoNaoEnviaRequisicao() {
        for (String valor : Arrays.asList("NaN", "-1", "1.001", "10000000000", "abc")) vm.salvar(null, "Mouse", "Periférico", "Sem fio", valor, true);
        assertEquals(0, repo.gravacoes);
    }
    @Test public void cadastroUsaCamposReaisSemIdERecarrega() {
        vm.salvar(null, " Mouse ", " Periférico ", " Sem fio ", "19,90", false);
        assertEquals(1, repo.gravacoes); assertNull(repo.enviado.id);
        assertEquals(new BigDecimal("19.90"), repo.enviado.valor); assertFalse(repo.enviado.disponivel);
        assertEquals("Mouse", repo.enviado.nome); assertEquals(2, repo.listagens);
        assertEquals(1, vm.getEstado().getValue().formularioSalvo);
    }
    @Test public void edicaoUsaIdNoCaminho() {
        vm.salvar(7L, "Mouse", "Periférico", "Sem fio", "20", true);
        assertEquals(Long.valueOf(7), repo.idAtualizado); assertNull(repo.enviado.id);
    }
    @Test public void erroAoSalvarPreservaFormularioEPermiteTentarNovamente() {
        repo.falhar = true; vm.salvar(null, "Mouse", "Periférico", "Sem fio", "20", true);
        assertEquals(0, vm.getEstado().getValue().formularioSalvo);
        assertFalse(vm.getEstado().getValue().ocupado);
        assertTrue(vm.getEstado().getValue().mensagem.contains("HTTP 400"));
    }
    @Test public void exclusaoRecarregaLista() {
        vm.excluir(7L); assertEquals(Long.valueOf(7), repo.idExcluido); assertEquals(2, repo.listagens);
    }
    @Test public void precoAlteradoImpedePedidoAteRevisao() {
        Produto p = repo.lista.get(0); vm.quantidade(p, 1);
        p.valor = new BigDecimal("25"); vm.confirmarPedido("Mesa 1", "");
        assertEquals(0, repo.pedidosCriados);
        assertEquals(new BigDecimal("25"), vm.getEstado().getValue().carrinho.get(0).precoUnitario);
        vm.confirmarPedido("Mesa 1", ""); assertEquals(1, repo.pedidosCriados);
        assertTrue(vm.getEstado().getValue().carrinho.isEmpty());
    }
    @Test public void indisponivelSaiDoCarrinhoENaoEnviaPedido() {
        Produto p = repo.lista.get(0); vm.quantidade(p, 1); p.disponivel = false;
        vm.confirmarPedido("Mesa 1", ""); assertEquals(0, repo.pedidosCriados);
        assertTrue(vm.getEstado().getValue().carrinho.isEmpty());
    }
    @Test public void quantidadeLimitadaANoventaENove() {
        for (int i = 0; i < 101; i++) vm.quantidade(repo.lista.get(0), 1);
        assertEquals(99, vm.getEstado().getValue().carrinho.get(0).quantidade);
    }
    @Test public void urlInvalidaNaoConfiguraRepositorio() {
        assertFalse(vm.configurar("http://example.test/"));
        assertFalse(vm.configurar("https://example.test/?x=1"));
        assertTrue(vm.configurar("http://10.0.2.2:8080"));
        assertEquals("http://10.0.2.2:8080/", vm.getUrl());
    }
    private static class FakeRepository extends ProdutoRepository {
        int gravacoes, listagens, pedidosCriados;
        boolean falhar;
        Long idAtualizado, idExcluido;
        Produto enviado;
        final List<Produto> lista = new ArrayList<>();
        FakeRepository() {
            Produto p = new Produto("Mouse", "Periférico", "Sem fio", new BigDecimal("20"), true);
            p.id = 7L; lista.add(p);
        }
        @Override public void configurar(String url) { }
        @Override public void listar(Retorno<List<Produto>> r) { listagens++; r.sucesso(lista); }
        @Override public void criar(Produto p, Retorno<Void> r) {
            gravacoes++; enviado = p; if (falhar) r.erro("HTTP 400"); else r.sucesso(null);
        }
        @Override public void atualizar(Long id, Produto p, Retorno<Void> r) { idAtualizado = id; criar(p, r); }
        @Override public void excluir(Long id, Retorno<Void> r) { idExcluido = id; r.sucesso(null); }
        @Override public void pedir(Pedido.Entrada p, Retorno<Pedido> r) {
            pedidosCriados++; Pedido pedido = new Pedido(); pedido.id = 1L; pedido.total = BigDecimal.TEN; r.sucesso(pedido);
        }
    }
}
