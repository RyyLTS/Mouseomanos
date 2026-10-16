# Histórias 2 e 3 — X-Squad Hamburgueria

O Sprint Backlog fornecido descreve modelar/criar o banco, criar e atualizar objetos (ID02), inserir e excluir objetos e oferecer uma interface de cadastro (ID03). Aqui, os objetos são os itens do cardápio: hambúrgueres, combos, acompanhamentos e bebidas.

| Tarefa do papel | Implementação no sistema |
| --- | --- |
| Modelar e criar banco de dados | Banco H2 persistente, criado pelo Spring Data JPA a partir das entidades. |
| Criar/inserir objeto no banco | **Gerenciar cardápio → Cadastrar item**, usando `POST /produtos`. |
| Atualizar objeto no banco | **Editar → Salvar alterações**, usando `PUT /produtos/{id}`. Altera nome, categoria, descrição, preço e disponibilidade. |
| Alterar preço | **Alterar preço → Salvar preço**, usando `PATCH /produtos/{id}/preco`. |
| Deletar objeto do banco | **Excluir → Excluir item**, usando `DELETE /produtos/{id}`. O registro sai da tabela e do cardápio. |
| Interface de cadastro dos objetos | Formulário e lista em **Gerenciar cardápio**, com busca, edição, cancelamento, validação e confirmação de exclusão. |

## Modelo do cadastro

Tabela `produto`:

| Coluna | Tipo | Uso |
| --- | --- | --- |
| `id` | BIGINT, chave primária gerada | Identificação do item |
| `nome` | VARCHAR(255) | Nome do hambúrguer, bebida ou acompanhamento |
| `categoria` | VARCHAR(255) | Categoria exibida no cardápio |
| `descricao` | VARCHAR(255) | Ingredientes e características |
| `valor` | NUMERIC(12,2) | Preço em reais, com duas casas decimais |
| `disponivel` | BOOLEAN | Define se aparece para pedidos |

O servidor exige nome, categoria e descrição preenchidos, até 255 caracteres, e preço não negativo com até duas casas decimais. Os dados ficam em `data/mouseomanos.mv.db`; o nome técnico do arquivo foi mantido para preservar os cadastros existentes após a mudança para X-Squad.

`pedido` e `pedido_item` preservam os pedidos já confirmados. Excluir um produto não apaga o histórico: os nomes e preços cobrados foram copiados para o pedido na confirmação.

## Como demonstrar

1. Abra **Gerenciar cardápio**, cadastre um hambúrguer com nome, categoria, descrição e preço.
2. Abra **Cardápio** e confira o novo item.
3. Use **Editar** para alterar seus dados, ou **Alterar preço** para mudar apenas o valor.
4. Confira o resultado no cardápio e recarregue a página: os dados continuam salvos.
5. Exclua o item e confira que ele saiu tanto do cadastro quanto do cardápio.
6. Reinicie com `parar.cmd` e `iniciar.cmd`: alterações e exclusões são mantidas.

Os dados de `exemplos/cardapio-xsquad.json` são sugestões de cardápio com preços ilustrativos. Não há reinserção automática desses exemplos na inicialização: um item excluído continua excluído.
