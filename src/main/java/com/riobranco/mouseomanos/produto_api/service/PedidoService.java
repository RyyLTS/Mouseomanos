package com.riobranco.mouseomanos.produto_api.service;

import com.riobranco.mouseomanos.produto_api.entity.*;
import com.riobranco.mouseomanos.produto_api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final ProdutoRepository produtos;
    public PedidoService(PedidoRepository pedidos, ProdutoRepository produtos) { this.pedidos = pedidos; this.produtos = produtos; }
    public record ItemEntrada(Long produtoId, Integer quantidade, BigDecimal precoUnitario) { }
    public record Entrada(String cliente, String observacao, List<ItemEntrada> itens) { }
    public record ItemSaida(Long produtoId, String nome, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) { }
    public record Saida(Long id, String cliente, String observacao, Instant criadoEm, String status, List<ItemSaida> itens, BigDecimal total) { }

    @Transactional
    public Saida criar(Entrada entrada) {
        if (entrada.cliente() == null || entrada.cliente().isBlank() || entrada.cliente().trim().length() > 100) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe o nome ou a mesa com até 100 caracteres.");
        }
        if (entrada.observacao() != null && entrada.observacao().length() > 500) throw erro(HttpStatus.BAD_REQUEST, "A observação deve ter até 500 caracteres.");
        if (entrada.itens() == null || entrada.itens().isEmpty() || entrada.itens().size() > 100) {
            throw erro(HttpStatus.BAD_REQUEST, "O pedido deve ter entre 1 e 100 itens diferentes.");
        }
        Set<Long> ids = new HashSet<>();
        for (ItemEntrada item : entrada.itens()) {
            if (item == null || item.produtoId() == null || item.produtoId() <= 0 || item.quantidade() == null || item.quantidade() < 1 || item.quantidade() > 99 || !ids.add(item.produtoId())) {
                throw erro(HttpStatus.BAD_REQUEST, "Informe itens diferentes e quantidades inteiras de 1 a 99.");
            }
        }
        // Lock in a consistent order so prices and availability stay valid throughout checkout.
        Map<Long, Produto> atuais = new HashMap<>();
        for (Long id : ids.stream().sorted().toList()) {
            Produto produto = produtos.buscarParaAlteracao(id).orElseThrow(() -> erro(HttpStatus.CONFLICT, "Um item foi excluído. Atualize o cardápio antes de pedir."));
            if (!produto.isDisponivel()) throw erro(HttpStatus.CONFLICT, "O item " + produto.getNome() + " não está disponível. Atualize o cardápio.");
            atuais.put(id, produto);
        }
        List<ItemPedido> itens = new ArrayList<>();
        for (ItemEntrada item : entrada.itens()) {
            Produto produto = atuais.get(item.produtoId());
            if (item.precoUnitario() != null && item.precoUnitario().compareTo(produto.getValor()) != 0) {
                throw erro(HttpStatus.CONFLICT, "O preço de " + produto.getNome() + " mudou. Confira o novo valor antes de confirmar.");
            }
            itens.add(new ItemPedido(produto, item.quantidade()));
        }
        return saida(pedidos.save(new Pedido(entrada.cliente().trim(), entrada.observacao() == null ? "" : entrada.observacao().trim(), itens)));
    }

    @Transactional(readOnly = true)
    public List<Saida> listar() { return pedidos.findAllByOrderByCriadoEmDescIdDesc().stream().map(this::saida).toList(); }

    @Transactional(readOnly = true)
    public Saida buscar(Long id) { return saida(pedidos.findById(id).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Pedido não encontrado."))); }

    private Saida saida(Pedido pedido) {
        return new Saida(pedido.getId(), pedido.getCliente(), pedido.getObservacao(), pedido.getCriadoEm(), "CONFIRMADO",
                pedido.getItens().stream().map(i -> new ItemSaida(i.getProdutoId(), i.getNome(), i.getQuantidade(), i.getPrecoUnitario(), i.getSubtotal())).toList(), pedido.getTotal());
    }
    private ResponseStatusException erro(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
}
