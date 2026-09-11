package com.riobranco.mouseomanos.produto_api.controller;

import com.riobranco.mouseomanos.produto_api.entity.Produto;
import com.riobranco.mouseomanos.produto_api.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    @Autowired
    private ProdutoRepository produtoRepository;

    // 1. CREATE - Cadastrar um novo produto (POST)
    @PostMapping
    public Produto criar(@RequestBody Produto produto) {
        return produtoRepository.save(produto);
    }

    // 2. READ - Listar todos os produtos (GET)
    @GetMapping
    public List<Produto> listar() {
        return produtoRepository.findAll();
    }

    // 2.1. READ por ID - Buscar um produto específico (GET)
    @GetMapping("/{id}")
    public Optional<Produto> buscarPorId(@PathVariable Long id) {
        return produtoRepository.findById(id);
    }

    // 3. UPDATE - Atualizar um produto existente (PUT)
    @PutMapping("/{id}")
    public Produto atualizar(@PathVariable Long id, @RequestBody Produto produtoDetalhes) {
        Produto produto = produtoRepository.findById(id).orElseThrow(() -> new RuntimeException("Produto não encontrado com o id: " + id));

        produto.setNome(produtoDetalhes.getNome());
        produto.setCategoria(produtoDetalhes.getCategoria());
        produto.setDescricao(produtoDetalhes.getDescricao());
        produto.setValor(produtoDetalhes.getValor());

        return produtoRepository.save(produto);
    }

    // 4. DELETE - Excluir um produto (DELETE)
    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        produtoRepository.deleteById(id);
    }
}