package com.riobranco.mouseomanos.produto_api.controller;

import com.riobranco.mouseomanos.produto_api.entity.Produto;
import com.riobranco.mouseomanos.produto_api.repository.ProdutoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {
    private final ProdutoRepository repository;
    public ProdutoController(ProdutoRepository repository) { this.repository = repository; }

    @PostMapping
    public Produto criar(@RequestBody Produto produto) {
        validar(produto);
        produto.setId(null);
        return repository.save(produto);
    }

    @GetMapping
    public List<Produto> listar() { return repository.findAll(); }

    @GetMapping("/{id}")
    public Produto buscarPorId(@PathVariable Long id) { return repository.findById(id).orElseThrow(this::naoEncontrado); }

    @PutMapping("/{id}")
    @Transactional
    public Produto atualizar(@PathVariable Long id, @RequestBody Produto dados) {
        validar(dados);
        Produto produto = repository.buscarParaAlteracao(id).orElseThrow(this::naoEncontrado);
        produto.setNome(dados.getNome());
        produto.setCategoria(dados.getCategoria());
        produto.setDescricao(dados.getDescricao());
        produto.setValor(dados.getValor());
        produto.setDisponivel(dados.isDisponivel());
        return repository.save(produto);
    }

    public record Preco(BigDecimal valor) { }

    @PatchMapping("/{id}/preco")
    @Transactional
    public Produto alterarPreco(@PathVariable Long id, @RequestBody Preco dados) {
        validarValor(dados.valor());
        Produto produto = repository.buscarParaAlteracao(id).orElseThrow(this::naoEncontrado);
        produto.setValor(dados.valor());
        return repository.save(produto);
    }

    public record Disponibilidade(Boolean disponivel) { }

    @PatchMapping("/{id}/disponibilidade")
    @Transactional
    public Produto alterarDisponibilidade(@PathVariable Long id, @RequestBody Disponibilidade dados) {
        if (dados.disponivel() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a disponibilidade do item.");
        Produto produto = repository.buscarParaAlteracao(id).orElseThrow(this::naoEncontrado);
        produto.setDisponivel(dados.disponivel());
        return repository.save(produto);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void deletar(@PathVariable Long id) { repository.delete(repository.buscarParaAlteracao(id).orElseThrow(this::naoEncontrado)); }

    private ResponseStatusException naoEncontrado() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado."); }

    private void validar(Produto produto) {
        produto.setNome(texto(produto.getNome(), "nome"));
        produto.setCategoria(texto(produto.getCategoria(), "categoria"));
        produto.setDescricao(texto(produto.getDescricao(), "descrição"));
        validarValor(produto.getValor());
    }

    private String texto(String valor, String campo) {
        if (valor == null || valor.isBlank() || valor.trim().length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preencha " + campo + " com até 255 caracteres.");
        }
        return valor.trim();
    }

    private void validarValor(BigDecimal valor) {
        if (valor == null || valor.signum() < 0 || valor.stripTrailingZeros().scale() > 2 || valor.precision() - valor.scale() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um preço válido, não negativo e com até duas casas decimais.");
        }
    }
}
