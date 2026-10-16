package com.riobranco.mouseomanos.produto_api.controller;

import com.riobranco.mouseomanos.produto_api.service.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    private final PedidoService service;
    public PedidoController(PedidoService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public PedidoService.Saida criar(@RequestBody PedidoService.Entrada entrada) { return service.criar(entrada); }
    @GetMapping
    public List<PedidoService.Saida> listar() { return service.listar(); }
    @GetMapping("/{id}")
    public PedidoService.Saida buscar(@PathVariable Long id) { return service.buscar(id); }
}
