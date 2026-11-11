package com.riobranco.mousseomanos;

import java.math.BigDecimal;

/** DTO Android: nomes iguais ao JSON do servidor; sem anotações JPA. */
public class Produto {
    public Long id;
    public String nome, categoria, descricao;
    public BigDecimal valor;
    public boolean disponivel = true;

    public Produto() { }
    public Produto(String nome, String categoria, String descricao, BigDecimal valor, boolean disponivel) {
        this.nome = nome; this.categoria = categoria; this.descricao = descricao;
        this.valor = valor; this.disponivel = disponivel;
    }
}
