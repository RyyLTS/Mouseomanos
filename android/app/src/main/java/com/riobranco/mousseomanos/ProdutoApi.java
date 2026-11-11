package com.riobranco.mousseomanos;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ProdutoApi {
    @GET("produtos") Call<List<Produto>> listar();
    @POST("produtos") Call<Void> criar(@Body Produto produto);
    @PUT("produtos/{id}") Call<Void> atualizar(@Path("id") Long id, @Body Produto produto);
    @DELETE("produtos/{id}") Call<Void> excluir(@Path("id") Long id);
    @GET("pedidos") Call<List<Pedido>> pedidos();
    @POST("pedidos") Call<Pedido> pedir(@Body Pedido.Entrada pedido);
}
