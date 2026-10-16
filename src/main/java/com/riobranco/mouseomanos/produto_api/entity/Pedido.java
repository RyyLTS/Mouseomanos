package com.riobranco.mouseomanos.produto_api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String cliente;
    @Column(length = 500)
    private String observacao;
    @Column(nullable = false)
    private Instant criadoEm;
    @ElementCollection
    @CollectionTable(name = "pedido_item", joinColumns = @JoinColumn(name = "pedido_id"))
    @OrderColumn(name = "posicao")
    private List<ItemPedido> itens = new ArrayList<>();
    protected Pedido() { }
    public Pedido(String cliente, String observacao, List<ItemPedido> itens) {
        this.cliente = cliente;
        this.observacao = observacao;
        this.itens = new ArrayList<>(itens);
        this.criadoEm = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
    public Long getId() { return id; }
    public String getCliente() { return cliente; }
    public String getObservacao() { return observacao; }
    public Instant getCriadoEm() { return criadoEm; }
    public List<ItemPedido> getItens() { return itens; }
    public BigDecimal getTotal() { return itens.stream().map(ItemPedido::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add); }
}
