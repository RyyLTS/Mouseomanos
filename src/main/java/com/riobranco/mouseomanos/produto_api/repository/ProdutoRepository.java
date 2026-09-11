package com.riobranco.mouseomanos.produto_api.repository;

import com.riobranco.mouseomanos.produto_api.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}