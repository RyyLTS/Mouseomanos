package com.riobranco.mousseomanos;

import java.math.BigDecimal;
import java.util.List;

public class Pedido {
    public Long id;
    public String cliente, observacao, criadoEm, status;
    public BigDecimal total;
    public List<Item> itens;
    public static class Item {
        public Long produtoId;
        public String nome;
        public int quantidade;
        public BigDecimal precoUnitario, subtotal;
    }
    public static class Entrada {
        public final String cliente, observacao;
        public final List<Item> itens;
        public Entrada(String cliente, String observacao, List<Item> itens) {
            this.cliente = cliente; this.observacao = observacao; this.itens = itens;
        }
    }
}
