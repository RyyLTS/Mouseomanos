package com.riobranco.mouseomanos.produto_api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Embeddable
public class ItemPedido {
    private Long produtoId;
    private String nome;
    private int quantidade;
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal precoUnitario;
    protected ItemPedido() { }
    public ItemPedido(Produto produto, int quantidade) {
        this.produtoId = produto.getId();
        this.nome = produto.getNome();
        this.precoUnitario = produto.getValor();
        this.quantidade = quantidade;
    }
    public Long getProdutoId() { return produtoId; }
    public String getNome() { return nome; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public BigDecimal getSubtotal() { return precoUnitario.multiply(BigDecimal.valueOf(quantidade)); }
}
