package com.riobranco.mousseomanos;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {
    private RetrofitClient() { }
    public static ProdutoApi criar(String url) {
        OkHttpClient cliente = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
                // Não repetir automaticamente gravações após falhas de conexão.
                .retryOnConnectionFailure(false).build();
        return new Retrofit.Builder().baseUrl(url).client(cliente)
                .addConverterFactory(GsonConverterFactory.create()).build().create(ProdutoApi.class);
    }
}
