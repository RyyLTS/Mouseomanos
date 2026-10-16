package com.riobranco.mouseomanos.produto_api.repository;

import com.riobranco.mouseomanos.produto_api.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findAllByOrderByCriadoEmDescIdDesc();
}
