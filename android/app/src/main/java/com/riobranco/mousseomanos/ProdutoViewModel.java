package com.riobranco.mousseomanos;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;

/** Estado e regras da tela. Não guarda Activity, View nem Context. */
public class ProdutoViewModel extends ViewModel {
    public static class Estado {
        public final List<Produto> produtos;
        public final List<Pedido> pedidos;
        public final List<Pedido.Item> carrinho;
        public final String mensagem;
        public final boolean ocupado;
        public final int formularioSalvo;
        Estado(List<Produto> p, List<Pedido> pedidos, Collection<Pedido.Item> c, String m, boolean b, int salvo) {
            produtos = Collections.unmodifiableList(new ArrayList<>(p));
            this.pedidos = Collections.unmodifiableList(new ArrayList<>(pedidos));
            carrinho = Collections.unmodifiableList(new ArrayList<>(c));
            mensagem = m; ocupado = b; formularioSalvo = salvo;
        }
    }
    private final ProdutoRepository repo;
    private final MutableLiveData<Estado> estado = new MutableLiveData<>();
    private List<Produto> produtos = new ArrayList<>();
    private List<Pedido> pedidos = new ArrayList<>();
    private final Map<Long, Pedido.Item> carrinho = new LinkedHashMap<>();
    private String url = "", mensagem = "Configure a URL da API para começar.";
    private boolean ocupado;
    private int formularioSalvo;
    public ProdutoViewModel() { this(new ProdutoRepository()); }
    ProdutoViewModel(ProdutoRepository repo) { this.repo = repo; emitir(); }
    public LiveData<Estado> getEstado() { return estado; }
    public String getUrl() { return url; }
    private void emitir() { estado.setValue(new Estado(produtos, pedidos, carrinho.values(), mensagem, ocupado, formularioSalvo)); }
    private void aviso(String msg) { mensagem = msg; emitir(); }
    private void erro(String msg) { ocupado = false; aviso(msg); }
    private boolean iniciar() {
        if (ocupado) return false;
        if (url.isEmpty()) { aviso("Configure a URL da API primeiro."); return false; }
        ocupado = true; emitir(); return true;
    }
    public boolean configurar(String texto) {
        if (ocupado) return false;
        String nova = texto.trim();
        try {
            URI uri = URI.create(nova);
            boolean local = Arrays.asList("localhost", "127.0.0.1", "10.0.2.2").contains(uri.getHost());
            if (uri.getHost() == null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || uri.getUserInfo() != null || !("https".equals(uri.getScheme()) || (local && "http".equals(uri.getScheme()))))
                throw new IllegalArgumentException();
            if (!nova.endsWith("/")) nova += "/";
            repo.configurar(nova);
        } catch (IllegalArgumentException ex) { aviso("Informe a URL raiz HTTPS da API, sem /produtos. No emulador: http://10.0.2.2:8080/"); return false; }
        url = nova; produtos = new ArrayList<>(); pedidos = new ArrayList<>(); carrinho.clear();
        carregar(); return true;
    }
    public void carregar() {
        if (!iniciar()) return;
        mensagem = "Carregando produtos..."; emitir(); listarDepois("");
    }
    private void listarDepois(String prefixo) {
        repo.listar(new ProdutoRepository.Retorno<List<Produto>>() {
            @Override public void sucesso(List<Produto> lista) {
                produtos = lista;
                boolean alterado = sincronizarCarrinho();
                ocupado = false;
                aviso(prefixo + lista.size() + " produto(s)." + (alterado ? " Carrinho atualizado: revise preços e itens." : ""));
            }
            @Override public void erro(String msg) { ProdutoViewModel.this.erro(prefixo + "Não foi possível atualizar a lista. " + msg); }
        });
    }
    public void salvar(Long id, String nome, String categoria, String descricao, String valor, boolean disponivel) {
        if (ocupado) return;
        for (String campo : Arrays.asList(nome, categoria, descricao)) {
            if (campo == null || campo.trim().isEmpty() || campo.trim().length() > 255) {
                aviso("Preencha nome, categoria e descrição com até 255 caracteres."); return;
            }
        }
        BigDecimal preco;
        try {
            preco = new BigDecimal(valor.trim().replace(',', '.'));
            if (preco.signum() < 0 || preco.stripTrailingZeros().scale() > 2 || preco.precision() - preco.scale() > 10)
                throw new NumberFormatException();
        } catch (NumberFormatException ex) { aviso("Informe um valor não negativo com até duas casas decimais."); return; }
        if (!iniciar()) return;
        Produto produto = new Produto(nome.trim(), categoria.trim(), descricao.trim(), preco, disponivel);
        ProdutoRepository.Retorno<Void> retorno = new ProdutoRepository.Retorno<Void>() {
            @Override public void sucesso(Void nada) {
                formularioSalvo++; emitir(); listarDepois(id == null ? "Cadastrado! " : "Atualizado! ");
            }
            @Override public void erro(String msg) { ProdutoViewModel.this.erro(msg); }
        };
        if (id == null) repo.criar(produto, retorno); else repo.atualizar(id, produto, retorno);
    }
    public void excluir(Long id) {
        if (id == null || !iniciar()) return;
        repo.excluir(id, new ProdutoRepository.Retorno<Void>() {
            @Override public void sucesso(Void nada) { carrinho.remove(id); listarDepois("Excluído! "); }
            @Override public void erro(String msg) { ProdutoViewModel.this.erro(msg); }
        });
    }
    public void quantidade(Produto p, int diferenca) {
        if (ocupado || p.id == null || !p.disponivel || p.valor == null) return;
        Pedido.Item atual = carrinho.get(p.id);
        int q = (atual == null ? 0 : atual.quantidade) + diferenca;
        if (q > 99) { aviso("Máximo de 99 unidades por produto."); return; }
        if (q <= 0) carrinho.remove(p.id);
        else {
            Pedido.Item novo = new Pedido.Item(); novo.produtoId = p.id; novo.nome = p.nome;
            novo.quantidade = q; novo.precoUnitario = p.valor; carrinho.put(p.id, novo);
        }
        emitir();
    }
    private boolean sincronizarCarrinho() {
        boolean mudou = false;
        for (Long id : new ArrayList<>(carrinho.keySet())) {
            Produto produto = null;
            for (Produto p : produtos) if (id.equals(p.id)) produto = p;
            if (produto == null || !produto.disponivel || produto.valor == null) { carrinho.remove(id); mudou = true; }
            else {
                Pedido.Item item = carrinho.get(id);
                if (item.precoUnitario.compareTo(produto.valor) != 0) mudou = true;
                Pedido.Item novo = new Pedido.Item(); novo.produtoId = id; novo.nome = produto.nome;
                novo.quantidade = item.quantidade; novo.precoUnitario = produto.valor; carrinho.put(id, novo);
            }
        }
        return mudou;
    }
    public void confirmarPedido(String cliente, String observacao) {
        if (cliente.trim().isEmpty() || cliente.trim().length() > 100 || observacao.length() > 500) {
            aviso("Informe nome ou mesa (até 100 caracteres) e observação com até 500 caracteres."); return;
        }
        if (carrinho.isEmpty()) { aviso("Adicione produtos ao carrinho."); return; }
        if (!iniciar()) return;
        // Confere o cardápio antes de enviar; o servidor também valida atomicamente.
        repo.listar(new ProdutoRepository.Retorno<List<Produto>>() {
            @Override public void sucesso(List<Produto> lista) {
                produtos = lista;
                if (sincronizarCarrinho()) { erro("O cardápio mudou. Revise o carrinho e confirme novamente."); return; }
                Pedido.Entrada entrada = new Pedido.Entrada(cliente.trim(), observacao.trim(), new ArrayList<>(carrinho.values()));
                repo.pedir(entrada, new ProdutoRepository.Retorno<Pedido>() {
                    @Override public void sucesso(Pedido p) {
                        carrinho.clear(); pedidos.add(0, p); ocupado = false;
                        aviso("Pedido #" + p.id + " confirmado! Total: R$ " + p.total);
                    }
                    @Override public void erro(String msg) {
                        ProdutoViewModel.this.erro(msg + " Se houve falha de conexão, consulte Pedidos antes de repetir a confirmação.");
                    }
                });
            }
            @Override public void erro(String msg) { ProdutoViewModel.this.erro(msg); }
        });
    }
    public void carregarPedidos() {
        if (!iniciar()) return;
        repo.pedidos(new ProdutoRepository.Retorno<List<Pedido>>() {
            @Override public void sucesso(List<Pedido> lista) { pedidos = new ArrayList<>(lista); ocupado = false; aviso(lista.size() + " pedido(s)."); }
            @Override public void erro(String msg) { ProdutoViewModel.this.erro(msg); }
        });
    }
    @Override protected void onCleared() { repo.cancelar(); }
}
