package com.riobranco.mousseomanos;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import retrofit2.*;

/** Model: só esta camada executa chamadas Retrofit. */
public class ProdutoRepository {
    public interface Retorno<T> { void sucesso(T dado); void erro(String mensagem); }
    private ProdutoApi api;
    private final List<Call<?>> pendentes = new ArrayList<>();

    public ProdutoRepository() { }
    ProdutoRepository(ProdutoApi api) { this.api = api; }
    public void configurar(String url) { cancelar(); api = RetrofitClient.criar(url); }
    public void listar(Retorno<List<Produto>> r) { executar(api.listar(), r, false); }
    public void criar(Produto p, Retorno<Void> r) { executar(api.criar(p), r, true); }
    public void atualizar(Long id, Produto p, Retorno<Void> r) { executar(api.atualizar(id, p), r, true); }
    public void excluir(Long id, Retorno<Void> r) { executar(api.excluir(id), r, true); }
    public void pedidos(Retorno<List<Pedido>> r) { executar(api.pedidos(), r, false); }
    public void pedir(Pedido.Entrada p, Retorno<Pedido> r) { executar(api.pedir(p), r, false); }
    public void cancelar() {
        for (Call<?> c : new ArrayList<>(pendentes)) c.cancel();
        pendentes.clear();
    }
    private <T> void executar(Call<T> call, Retorno<T> r, boolean permiteVazio) {
        pendentes.add(call);
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(Call<T> c, Response<T> resposta) {
                pendentes.remove(c);
                if (c.isCanceled()) return;
                if (resposta.isSuccessful() && (permiteVazio || resposta.body() != null)) r.sucesso(resposta.body());
                else {
                    String mensagem = "A API respondeu com erro HTTP " + resposta.code();
                    try {
                        if (resposta.errorBody() != null) {
                            var json = JsonParser.parseString(resposta.errorBody().string()).getAsJsonObject();
                            if (json.has("message")) mensagem += ": " + json.get("message").getAsString();
                        }
                    } catch (Exception ignorado) { /* Resposta pode não ser JSON. */ }
                    r.erro(resposta.isSuccessful() ? "A API retornou uma resposta vazia." : mensagem);
                }
            }
            @Override public void onFailure(Call<T> c, Throwable erro) {
                pendentes.remove(c);
                if (!c.isCanceled()) r.erro("Sem conexão com a API. Confira a URL e a internet. " + erro.getMessage());
            }
        });
    }
}
