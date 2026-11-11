package com.riobranco.mousseomanos;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** View: desenha o estado e encaminha ações. Não conhece HTTP nem Repository. */
public class MainActivity extends AppCompatActivity {
    private ProdutoViewModel vm;
    private ProdutoViewModel.Estado estado;
    private Long idEmEdicao;
    private String aba = "cardapio";
    private int ultimoSalvo;
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private EditText campo(int id) { return findViewById(id); }
    private String texto(int id) { return campo(id).getText().toString(); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View raiz = findViewById(R.id.main);
        int padding = raiz.getPaddingLeft();
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom);
            return insets;
        });
        vm = new ViewModelProvider(this).get(ProdutoViewModel.class);
        if (savedInstanceState != null) {
            aba = savedInstanceState.getString("aba", "cardapio");
            if (savedInstanceState.containsKey("id")) idEmEdicao = savedInstanceState.getLong("id");
            ultimoSalvo = savedInstanceState.getInt("salvo");
        }
        findViewById(R.id.configurar).setOnClickListener(v -> configurar());
        findViewById(R.id.cardapio).setOnClickListener(v -> abrir("cardapio"));
        findViewById(R.id.gerenciar).setOnClickListener(v -> abrir("gerenciar"));
        findViewById(R.id.carrinho).setOnClickListener(v -> abrir("carrinho"));
        findViewById(R.id.pedidos).setOnClickListener(v -> { abrir("pedidos"); vm.carregarPedidos(); });
        findViewById(R.id.atualizar).setOnClickListener(v -> {
            if (aba.equals("pedidos")) vm.carregarPedidos(); else vm.carregar();
        });
        findViewById(R.id.salvar).setOnClickListener(v -> vm.salvar(idEmEdicao, texto(R.id.nome), texto(R.id.categoria),
                texto(R.id.descricao), texto(R.id.valor), ((CheckBox) findViewById(R.id.disponivel)).isChecked()));
        findViewById(R.id.limpar).setOnClickListener(v -> limpar());
        findViewById(R.id.confirmar).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Confirmar pedido?").setMessage("Confira os itens e o total antes de enviar.")
                .setNegativeButton("Voltar", null).setPositiveButton("Enviar", (d, w) ->
                    vm.confirmarPedido(texto(R.id.cliente), texto(R.id.observacao))).show());
        campo(R.id.busca).addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            public void afterTextChanged(Editable e) { }
        });
        vm.getEstado().observe(this, novo -> {
            estado = novo;
            if (novo.formularioSalvo != ultimoSalvo) { ultimoSalvo = novo.formularioSalvo; limpar(); }
            render();
        });
        if (vm.getUrl().isEmpty()) {
            String url = getPreferences(MODE_PRIVATE).getString("api", getString(R.string.api_padrao));
            if (!url.isEmpty()) vm.configurar(url);
        }
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("aba", aba); out.putInt("salvo", ultimoSalvo);
        if (idEmEdicao != null) out.putLong("id", idEmEdicao);
    }
    private void configurar() {
        EditText entrada = new EditText(this);
        entrada.setSingleLine(true); entrada.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        entrada.setHint("https://seu-app.azurewebsites.net/"); entrada.setText(vm.getUrl());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("URL raiz da API")
                .setMessage("Use o domínio do Azure, sem /produtos. Emulador local: http://10.0.2.2:8080/")
                .setView(entrada).setNegativeButton("Cancelar", null).setPositiveButton("Conectar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (vm.configurar(entrada.getText().toString())) {
                getPreferences(MODE_PRIVATE).edit().putString("api", vm.getUrl()).apply(); limpar(); dialog.dismiss();
            } else entrada.setError("Confira a URL e a mensagem na tela.");
        }));
        dialog.show();
    }
    private void abrir(String nome) { aba = nome; render(); }
    private void limpar() {
        idEmEdicao = null;
        for (int id : new int[]{R.id.nome, R.id.categoria, R.id.descricao, R.id.valor}) campo(id).setText("");
        ((CheckBox) findViewById(R.id.disponivel)).setChecked(true);
        ((TextView) findViewById(R.id.tituloFormulario)).setText("Novo produto");
        ((Button) findViewById(R.id.salvar)).setText("Cadastrar");
    }
    private void editar(Produto p) {
        idEmEdicao = p.id;
        campo(R.id.nome).setText(p.nome); campo(R.id.categoria).setText(p.categoria);
        campo(R.id.descricao).setText(p.descricao); campo(R.id.valor).setText(p.valor == null ? "" : p.valor.toPlainString());
        ((CheckBox) findViewById(R.id.disponivel)).setChecked(p.disponivel);
        render(); campo(R.id.nome).requestFocus();
    }
    private void habilitar(View view, boolean habilitado) {
        view.setEnabled(habilitado);
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) habilitar(((ViewGroup) view).getChildAt(i), habilitado);
    }
    private void visivel(int id, boolean sim) { findViewById(id).setVisibility(sim ? View.VISIBLE : View.GONE); }
    private LinearLayout bloco(LinearLayout pai, String titulo, String descricao) {
        LinearLayout bloco = new LinearLayout(this); bloco.setOrientation(LinearLayout.VERTICAL);
        bloco.setPadding(8, 16, 8, 16); pai.addView(bloco);
        TextView nome = new TextView(this); nome.setText(titulo); nome.setTextSize(20); bloco.addView(nome);
        TextView detalhe = new TextView(this); detalhe.setText(descricao); bloco.addView(detalhe);
        return bloco;
    }
    private void botao(LinearLayout pai, String texto, Runnable acao) {
        Button b = new Button(this); b.setText(texto); b.setEnabled(!estado.ocupado);
        b.setOnClickListener(v -> acao.run()); pai.addView(b);
    }
    private String dinheiro(BigDecimal valor) { return moeda.format(valor == null ? BigDecimal.ZERO : valor); }
    private void render() {
        if (estado == null) return;
        ((TextView) findViewById(R.id.status)).setText(estado.mensagem);
        visivel(R.id.progresso, estado.ocupado);
        boolean gerenciar = aba.equals("gerenciar"), carrinho = aba.equals("carrinho"), pedidos = aba.equals("pedidos");
        visivel(R.id.formulario, gerenciar); visivel(R.id.checkout, carrinho); visivel(R.id.busca, !carrinho && !pedidos);
        habilitar(findViewById(R.id.formulario), !estado.ocupado);
        for (int id : new int[]{R.id.configurar, R.id.atualizar, R.id.confirmar}) findViewById(id).setEnabled(!estado.ocupado);
        ((TextView) findViewById(R.id.tituloFormulario)).setText(idEmEdicao == null ? "Novo produto" : "Editar produto #" + idEmEdicao);
        ((Button) findViewById(R.id.salvar)).setText(idEmEdicao == null ? "Cadastrar" : "Atualizar produto");
        LinearLayout lista = findViewById(R.id.itens); lista.removeAllViews();
        if (pedidos) {
            for (Pedido p : estado.pedidos) {
                StringBuilder detalhe = new StringBuilder(p.cliente + " · " + p.criadoEm + "\n" + p.status + " · " + dinheiro(p.total));
                if (p.itens != null) for (Pedido.Item item : p.itens) detalhe.append("\n").append(item.quantidade).append(" × ").append(item.nome).append(" · ").append(dinheiro(item.precoUnitario));
                if (p.observacao != null && !p.observacao.isEmpty()) detalhe.append("\n").append(p.observacao);
                bloco(lista, "Pedido #" + p.id, detalhe.toString());
            }
        } else if (carrinho) {
            BigDecimal total = BigDecimal.ZERO;
            for (Pedido.Item item : estado.carrinho) {
                BigDecimal subtotal = item.precoUnitario.multiply(BigDecimal.valueOf(item.quantidade)); total = total.add(subtotal);
                LinearLayout card = bloco(lista, item.nome, item.quantidade + " × " + dinheiro(item.precoUnitario) + " = " + dinheiro(subtotal));
                Produto produto = null; for (Produto p : estado.produtos) if (item.produtoId.equals(p.id)) produto = p;
                if (produto != null) {
                    Produto p = produto; botao(card, "− Remover uma unidade", () -> vm.quantidade(p, -1));
                    botao(card, "+ Adicionar uma unidade", () -> vm.quantidade(p, 1));
                }
            }
            ((TextView) findViewById(R.id.total)).setText("Total: " + dinheiro(total));
            findViewById(R.id.confirmar).setEnabled(!estado.ocupado && !estado.carrinho.isEmpty());
        } else {
            String busca = texto(R.id.busca).trim().toLowerCase(new Locale("pt", "BR"));
            for (Produto p : estado.produtos) {
                if (!gerenciar && !p.disponivel) continue;
                if (!(p.nome + " " + p.categoria + " " + p.descricao).toLowerCase(new Locale("pt", "BR")).contains(busca)) continue;
                LinearLayout card = bloco(lista, p.nome, p.categoria + "\n" + p.descricao + "\n" + dinheiro(p.valor) + (p.disponivel ? "" : " · Indisponível"));
                if (gerenciar) {
                    botao(card, "Editar " + p.nome, () -> editar(p));
                    botao(card, "Excluir " + p.nome, () -> new AlertDialog.Builder(this).setTitle("Excluir produto?")
                            .setMessage(p.nome).setNegativeButton("Cancelar", null)
                            .setPositiveButton("Excluir", (d, w) -> { if (p.id.equals(idEmEdicao)) limpar(); vm.excluir(p.id); }).show());
                } else botao(card, "Adicionar ao carrinho", () -> vm.quantidade(p, 1));
            }
        }
        if (lista.getChildCount() == 0) bloco(lista, "Nenhum item", "Atualize a lista ou use outra busca.");
    }
}
