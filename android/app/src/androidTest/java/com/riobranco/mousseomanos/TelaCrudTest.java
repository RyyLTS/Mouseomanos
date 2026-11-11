package com.riobranco.mousseomanos;

import android.content.Context;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TelaCrudTest {
    private MockWebServer server;
    private ActivityScenario<MainActivity> tela;
    private final AtomicInteger listagens = new AtomicInteger();
    private final AtomicInteger gravacoes = new AtomicInteger();
    private volatile boolean falhar;
    private volatile boolean cadastrado;
    @Before public void abrir() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override public MockResponse dispatch(RecordedRequest req) {
                if ("POST".equals(req.getMethod())) {
                    gravacoes.incrementAndGet();
                    if (falhar) return new MockResponse().setResponseCode(400).setBody("{\"message\":\"Falha de teste\"}");
                    cadastrado = true; return new MockResponse().setResponseCode(201).setBody("{}");
                }
                listagens.incrementAndGet();
                return new MockResponse().setBody(cadastrado
                        ? "[{\"id\":1,\"nome\":\"Mouse teste\",\"categoria\":\"Periférico\",\"descricao\":\"Sem fio\",\"valor\":19.90,\"disponivel\":true}]" : "[]");
            }
        });
        server.start();
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).edit().clear()
                .putString("api", server.url("/").toString()).commit();
        tela = ActivityScenario.launch(MainActivity.class);
        esperarMensagem("0 produto(s)");
    }
    @After public void fechar() throws Exception { if (tela != null) tela.close(); server.shutdown(); }
    private void esperarMensagem(String trecho) throws Exception {
        long fim = System.currentTimeMillis() + 10000;
        AtomicBoolean ok = new AtomicBoolean();
        while (System.currentTimeMillis() < fim) {
            tela.onActivity(a -> ok.set(((TextView) a.findViewById(R.id.status)).getText().toString().contains(trecho)));
            if (ok.get()) return;
            Thread.sleep(50);
        }
        fail("A tela não apresentou: " + trecho);
    }
    private void preencher() {
        tela.onActivity(a -> {
            a.findViewById(R.id.gerenciar).performClick();
            ((EditText) a.findViewById(R.id.nome)).setText("Mouse teste");
            ((EditText) a.findViewById(R.id.categoria)).setText("Periférico");
            ((EditText) a.findViewById(R.id.descricao)).setText("Sem fio");
            ((EditText) a.findViewById(R.id.valor)).setText("19.90");
        });
    }
    @Test public void cadastroAtualizaListaERecriacaoPreservaEstadoSemNovoGet() throws Exception {
        preencher(); tela.onActivity(a -> a.findViewById(R.id.salvar).performClick());
        esperarMensagem("Cadastrado! 1 produto(s)");
        assertEquals(1, gravacoes.get()); assertEquals(2, listagens.get());
        tela.onActivity(a -> assertEquals("", ((EditText) a.findViewById(R.id.nome)).getText().toString()));
        tela.recreate(); esperarMensagem("Cadastrado! 1 produto(s)");
        assertEquals(2, listagens.get());
    }
    @Test public void erroPreservaCamposEValidacaoNaoChamaApi() throws Exception {
        preencher();
        tela.onActivity(a -> { ((EditText) a.findViewById(R.id.nome)).setText(""); a.findViewById(R.id.salvar).performClick(); });
        esperarMensagem("Preencha nome"); assertEquals(0, gravacoes.get());
        falhar = true;
        tela.onActivity(a -> { ((EditText) a.findViewById(R.id.nome)).setText("Mouse teste"); a.findViewById(R.id.salvar).performClick(); });
        esperarMensagem("Falha de teste");
        tela.recreate(); esperarMensagem("Falha de teste");
        tela.onActivity(a -> assertEquals("Mouse teste", ((EditText) a.findViewById(R.id.nome)).getText().toString()));
        assertEquals(1, gravacoes.get()); assertEquals(1, listagens.get());
    }
}
