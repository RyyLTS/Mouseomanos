# X-Squad — Hamburgueria

Sistema da hamburgueria **X-Squad**, com cadastro de hambúrgueres, combos, acompanhamentos e bebidas, cardápio e pedidos. Usa Spring Boot 4.1.1, Java 17, Spring Data JPA e H2. A interface é servida pela própria aplicação; não precisa de Node.js, MySQL ou frontend separado para executar.

O site tem tema escuro e vermelho, fotografias de hambúrgueres, vitrine com setas e miniaturas, filtros por categoria e carrinho em painel lateral. O cadastro está em **Gerenciar cardápio**, junto à busca e no rodapé. Veja [design, arquivos de imagem e prompts](docs/design-e-imagens.md).

## Executar no Windows

1. Abra `iniciar.cmd` com dois cliques, ou execute `./iniciar.cmd` no terminal desta pasta.
2. Aguarde a compilação e os testes. O navegador abre em http://127.0.0.1:8080.
3. Execute `parar.cmd` para encerrar o servidor em segundo plano.

O iniciador procura um JDK 17 ou superior e configura o Java apenas no processo atual. Neste computador, usa `C:\Program Files\Java\jdk-17`. O Maven e as dependências ficam em `.maven-cache/`. A primeira execução precisa de internet para baixá-los.

Se a porta estiver ocupada, encerre a outra aplicação ou use `./iniciar.cmd -Porta 8081`. Os logs ficam em `.runtime-tmp/servidor.log` e `.runtime-tmp/servidor-erros.log`.

Para compilar e testar sem iniciar o servidor:

```powershell
./iniciar.cmd -SomenteCompilar
```

## Histórias implementadas

O detalhamento das tarefas do Sprint Backlog, do modelo do banco e da demonstração das histórias 2 e 3 está em [docs/historias-2-e-3.md](docs/historias-2-e-3.md). O cardápio de exemplo está em [exemplos/cardapio-xsquad.json](exemplos/cardapio-xsquad.json), com preços ilustrativos editáveis no sistema.

| História | Critérios da imagem | Como usar |
| --- | --- | --- |
| ID01 | Visualizar cardápio previamente cadastrado | Abra **Cardápio**. Pesquise por nome, descrição ou categoria e filtre por categoria. Os itens disponíveis são carregados do banco. |
| ID02 | Alterar preços e visualizar preços atualizados no cardápio | Em **Gerenciar cardápio**, use **Alterar preço** ou **Editar**. O novo valor aparece no cardápio e no carrinho. |
| ID03 | Itens excluídos não aparecem; novos itens aparecem no cardápio | Cadastre ou exclua em **Gerenciar cardápio**. Também é possível indisponibilizar um item sem excluir seu cadastro. |
| ID04 | Pedir apenas itens disponíveis e validar a criação dos pedidos | Adicione itens no **Cardápio**, ajuste as quantidades, preencha nome ou mesa e confirme. O sistema mostra o número e o total; consulte os detalhes em **Pedidos**. |

As alterações aparecem imediatamente nesta interface e são buscadas automaticamente a cada 10 segundos nas demais abas visíveis. Também há botões para atualizar manualmente. Itens indisponíveis ou excluídos saem do carrinho. Se um preço mudar, o cliente deve revisar o novo total antes de confirmar.

O servidor verifica disponibilidade, quantidades e preços novamente no momento da confirmação. Um pedido com qualquer item inválido é recusado por inteiro. Os totais usam valores decimais exatos e são calculados no servidor. Cada pedido preserva o nome e o preço cobrados, mesmo após alterações ou exclusões no cardápio.

## Dados locais

O cardápio e os pedidos ficam no arquivo `data/mouseomanos.mv.db` e sobrevivem a reinicializações. A pasta `data/` não deve ser apagada; para fazer backup, encerre o servidor e copie essa pasta. O carrinho ainda não confirmado permanece apenas na aba atual.

A conexão usa `WRITE_DELAY=0` para gravar as transações sem a espera padrão do H2. Isso foi verificado confirmando um pedido e encerrando imediatamente o Java, depois consultando o mesmo pedido ao reiniciar. Referência: [configuração WRITE_DELAY do H2](https://h2database.com/html/commands.html#set_write_delay).

O iniciador escuta apenas em `127.0.0.1`. As telas de gestão e o histórico são compartilhados no computador e não têm autenticação. Os testes usam bancos em memória separados e não alteram os dados reais.

O console H2 está em http://127.0.0.1:8080/h2-console. URL JDBC: `jdbc:h2:file:./data/mouseomanos`; usuário `sa`; senha vazia.

## API

| Método | Caminho | Operação |
| --- | --- | --- |
| GET | `/cardapio` | Consultar itens disponíveis e preços atuais |
| GET / POST | `/produtos` | Listar todos os itens / cadastrar |
| GET / PUT / DELETE | `/produtos/{id}` | Consultar / editar / excluir item |
| PATCH | `/produtos/{id}/preco` | Alterar apenas o preço: `{"valor":15.90}` |
| PATCH | `/produtos/{id}/disponibilidade` | Alterar disponibilidade: `{"disponivel":false}` |
| GET / POST | `/pedidos` | Consultar histórico / criar pedido |
| GET | `/pedidos/{id}` | Consultar a confirmação e os detalhes de um pedido |

Cadastro ou edição de item:

```json
{"nome":"X-Bacon","categoria":"Hambúrgueres","descricao":"Pão brioche, hambúrguer de 160 g, cheddar e bacon crocante","valor":29.90,"disponivel":true}
```

Criação de pedido (retorna HTTP 201 com número, data, itens e total):

```json
{"cliente":"Ana · Mesa 4","observacao":"Sem cebola","itens":[{"produtoId":1,"quantidade":2,"precoUnitario":12.50}]}
```

O preço enviado no pedido serve para detectar mudanças desde a visualização do carrinho; o preço cobrado sempre vem do banco. Quantidades devem ser inteiras, de 1 a 99 por item, sem IDs repetidos. O campo `precoUnitario` pode ser omitido por integrações que desejem aceitar o preço atual do servidor.

Respostas de erro incluem `message`: HTTP 400 para dados inválidos, 404 para consultas a registros inexistentes e 409 quando um item do pedido foi excluído, ficou indisponível ou teve o preço alterado.

## Verificação

Os testes HTTP cobrem as quatro histórias, o CRUD existente, a interface estática, a disponibilidade, os cálculos monetários, a preservação do histórico, as alterações de preço durante a compra e a rejeição de pedidos vazios, itens duplicados, quantidades fracionárias e preços inválidos.
