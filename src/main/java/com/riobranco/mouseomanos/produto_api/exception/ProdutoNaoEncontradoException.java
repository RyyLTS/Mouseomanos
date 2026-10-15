package com.riobranco.mouseomanos.produto_api.exception;

public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(Long id) {
        super("Produto não encontrado com o id: " + id);
    }
}
