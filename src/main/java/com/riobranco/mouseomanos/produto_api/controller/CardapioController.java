package com.riobranco.mouseomanos.produto_api.controller;

import com.riobranco.mouseomanos.produto_api.entity.Produto;
import com.riobranco.mouseomanos.produto_api.repository.ProdutoRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/cardapio")
public class CardapioController {
    private final ProdutoRepository repository;
    public CardapioController(ProdutoRepository repository) { this.repository = repository; }
    @GetMapping
    public List<Produto> listar() { return repository.findByDisponivelTrueOrderByCategoriaAscNomeAsc(); }
}
