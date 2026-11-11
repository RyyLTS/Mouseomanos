# Mousseomanos Android — Aula 10, Trilha A

App nativo Java + Views/XML, seguindo a **Trilha A** do roteiro da Aula 10.
O projeto Android fica nesta pasta; o Spring Boot na raiz continua sendo a API.
A referência recebida no GitHub usa JavaScript puro, não Angular. Foram transportados os fluxos
de produtos, cardápio, carrinho e pedidos; o layout Android é nativo, não uma WebView.

## Abrir e executar

1. No Android Studio, use **Open** e selecione esta pasta `android` (não a raiz Maven).
2. Use JDK 17 ou 21 para o Gradle, Android SDK 35 e aceite o Sync.
3. Selecione aparelho Android 8.0/API 26 ou superior, ou emulador, e clique Run.
4. O app inicia com a URL informada pela squad:
   `https://api-mousseomanos-cwcgh6ghh9gvh8cj.brazilsouth-01.azurewebsites.net/`.
   A consulta `GET /produtos` respondeu HTTP 200 durante a validação.
   Para trocar o servidor, toque **Conectar à API**. Não inclua `/produtos` na URL raiz.
5. A URL fica salva no aparelho. Para testar com o servidor no computador e emulador,
   use `http://10.0.2.2:8080/` após iniciar a API com `iniciar.cmd` na raiz.
   Outra opção no aparelho por USB é `adb reverse tcp:8080 tcp:8080` e `http://localhost:8080/`.

HTTP sem TLS só é permitido para localhost e o endereço do host do emulador.
O iniciador do servidor escuta em loopback; um IP da rede local não funciona sem mudar essa configuração.
A API deve oferecer `/produtos` e, para carrinho/histórico, `/pedidos` conforme a versão do GitHub.
O Azure pode estar com uma versão anterior; um 404 em Pedidos precisa ser verificado no servidor.

## O que mudou e por quê

```text
MainActivity + activity_main.xml (View)
                ↓ ações   ↑ LiveData<Estado>
ProdutoViewModel (ViewModel)
                ↓
ProdutoRepository (Model / acesso a dados)
                ↓
ProdutoApi + RetrofitClient → API REST do Azure
```

| Arquivo | Responsabilidade |
| --- | --- |
| `MainActivity.java` + XML | Mostrar estado, ler campos, confirmação de exclusão/pedido e encaminhar eventos. Sem Retrofit, HTTP ou Repository. |
| `ProdutoViewModel.java` | Manter lista, carrinho, pedidos, mensagens e carregamento; validar campos; escolher POST ou PUT e atualizar a lista após gravar. |
| `ProdutoRepository.java` | Executar Retrofit, traduzir falhas HTTP/rede e devolver sucesso/erro por callback. Cancelar chamadas quando o ViewModel termina. |
| `ProdutoApi.java` | Declarar métodos, caminhos e JSON esperados pelo servidor. |
| `RetrofitClient.java` | Construir cliente e definir timeouts. Repetição automática de requisições desativada. |
| `Produto.java`, `Pedido.java` | Representar o JSON. Não são entidades JPA do servidor. |

O `ProdutoRepository` **do Android** acessa a API. O `ProdutoRepository` **Spring Data**
na raiz do projeto acessa o banco de dados. São camadas diferentes com o mesmo nome de papel.

O exemplo da apostila usa nome/preço/quantidade. Aqui a Entity real é `Produto`, o caminho é
`produtos` e os campos são `nome`, `categoria`, `descricao`, `valor` e `disponivel`.
Não foi inventado um campo de estoque: quantidade pertence ao carrinho/pedido, não ao Produto.
O preço usa `BigDecimal`, como o servidor, para evitar erros de ponto flutuante em dinheiro.

`new ViewModelProvider(this).get(ProdutoViewModel.class)` reaproveita o ViewModel na rotação.
`LiveData` permite que a Activity observe as mudanças respeitando seu ciclo de vida.
A Activity não guarda a lista nem executa chamadas HTTP. O id em edição e a aba são salvos no Bundle;
os campos do formulário possuem ids e usam a restauração normal de estado do Android.
O ViewModel mantém o carrinho durante a rotação; ele não é persistido em disco após encerrar o processo.

O formulário só é limpo após sucesso da gravação. Falhas e validações preservam o que foi digitado.
Botões de gravação ficam bloqueados durante requisições para evitar envios simultâneos.
Depois de cadastrar, editar ou excluir, a lista é buscada de novo na API, conforme o roteiro.
Se essa segunda consulta falhar, a mensagem informa a gravação concluída e a falha de atualização.

## Funcionalidades

- **Cardápio:** produtos disponíveis e busca por nome, categoria ou descrição; adicionar ao carrinho.
- **Gerenciar:** cadastro, edição de preço e campos, disponibilidade, exclusão com confirmação.
- **Carrinho:** aumentar/diminuir quantidades (1 a 99), total, cliente/mesa e observações.
- **Confirmar pedido:** reler produtos antes de enviar. Mudanças de preço ou disponibilidade interrompem
  o envio e pedem revisão. O servidor valida de novo na transação e calcula o total definitivo.
- **Pedidos:** histórico, número, status, data, itens, observação e total retornados pela API.
- **Atualizar:** atualização manual; não há polling a cada 10 segundos como na interface web.

Após falha de conexão na confirmação de pedido, confira o histórico antes de reenviar:
o servidor pode ter gravado mesmo quando a resposta não chegou. Não foi adicionado endpoint de
idempotência ao back-end. Gestão e pedidos continuam sem autenticação, como na API existente.

## Compilar e testar

No terminal desta pasta, com `JAVA_HOME` apontando para um JDK 17/21 e `ANDROID_HOME` para o SDK:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
Relatórios: `app/build/reports/tests/testDebugUnitTest/index.html` e
`app/build/reports/lint-results-debug.html`.

Os testes unitários verificam validação, cadastro/edição, recarga após exclusão, erro sem limpar
formulário, limites do carrinho e revisão de preço/disponibilidade. Os testes com MockWebServer
verificam caminhos HTTP, JSON, preço decimal, resposta vazia e mensagens do servidor.
Esses testes não substituem a prova no aparelho e no Azure.

Para repetir os testes de tela com emulador ou aparelho conectado:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Os testes de tela usam uma API HTTP simulada dentro do emulador; não gravam dados no Azure.
Verificam cadastro, atualização da lista, validação, preservação do formulário após erro e
recriação da Activity sem repetir a consulta. Para investigar falhas reais, abra o Logcat no
Android Studio, selecione o processo `com.riobranco.mousseomanos` e filtre por `level:ERROR`.

## Demonstração para a Aula 11

1. Configure a URL real e confira `GET /produtos` no Postman.
2. Cadastre um produto no app; prove sua existência pelo GET do Azure.
3. Edite valor/descrição e comprove pelo GET.
4. Exclua com confirmação e comprove que o registro saiu.
5. Tente cadastrar com nome vazio ou valor inválido: nenhuma gravação deve ser enviada.
6. Gire o celular: lista/carrinho continuam; não deve ocorrer uma nova consulta só por girar.
7. Em modo avião, tente atualizar/salvar: deve aparecer erro e os campos devem permanecer.
8. Demonstre onde estão View, ViewModel e Repository e explique o fluxo acima.
9. Faça commit e push da branch do Android após revisar o código; nenhum deploy da API é necessário.

O roteiro da Aula 10 foi consultado. A apostila de leitura de apoio citada no enunciado não estava disponível.
Documentação de apoio:
- https://developer.android.com/topic/libraries/architecture/viewmodel
- https://developer.android.com/topic/libraries/architecture/livedata
