# Mouseomanos

Sistema de gerenciamento de pedidos para restaurantes, com foco em hamburguerias.

O objetivo do projeto é centralizar a operação do estabelecimento: cardápio, pedidos e acompanhamento do que está sendo vendido. Este repositório contém o **back-end (API REST)** e um **front-end estático** servido pela própria aplicação.

## Status atual

O projeto está em desenvolvimento inicial. Hoje está implementado o módulo de **produtos** (itens do cardápio), que é a base para os pedidos.

| Módulo | Situação |
|---|---|
| Produtos (CRUD, validação, tratamento de erros) | Implementado |
| Interface web para cadastro de produtos | Implementada |
| Pedidos | Planejado |
| Persistência em banco de dados permanente | Planejado |

## Tecnologias

- Java 17
- Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA, Bean Validation)
- H2 Database (em memória)
- Maven (wrapper incluso: `mvnw`)
- JUnit 5 e MockMvc para testes
- Front-end em HTML, CSS e JavaScript puro
- GitHub Actions para build e deploy no Azure Web App

## Como executar

### Pré-requisitos

- JDK 17 ou superior
- Git

Não é necessário instalar o Maven; o wrapper (`mvnw`) baixa a versão correta.

### Build e testes

```bash
./mvnw clean install
```

Esse comando compila, executa os testes e gera o JAR em `target/`.

Para rodar apenas os testes:

```bash
./mvnw test
```

### Executar a aplicação

```bash
./mvnw spring-boot:run
```

Ou, a partir do JAR gerado:

```bash
java -jar target/produto-api-0.0.1-SNAPSHOT.jar
```

A aplicação sobe em `http://localhost:8080`.

| Recurso | URL |
|---|---|
| Interface web | http://localhost:8080 |
| API | http://localhost:8080/produtos |
| Console do H2 | http://localhost:8080/h2-console |

No console do H2, use a JDBC URL `jdbc:h2:mem:testdb`, usuário `SA` e senha vazia.

> No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

## API

Base: `/produtos`

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| `POST` | `/produtos` | Cadastra um produto | 200 |
| `GET` | `/produtos` | Lista todos os produtos | 200 |
| `GET` | `/produtos/{id}` | Busca um produto por id | 200 |
| `PUT` | `/produtos/{id}` | Atualiza um produto | 200 |
| `DELETE` | `/produtos/{id}` | Remove um produto | 204 |

### Modelo de produto

```json
{
  "categoria": "Hambúrguer",
  "nome": "X-Bacon",
  "descricao": "Pão brioche, blend 160g, bacon e queijo cheddar",
  "valor": 32.90
}
```

### Regras de validação

| Campo | Regra |
|---|---|
| `categoria` | Obrigatória, até 100 caracteres |
| `nome` | Obrigatório, até 150 caracteres |
| `descricao` | Opcional, até 500 caracteres |
| `valor` | Obrigatório e maior que zero |

### Formato de erro

Todos os erros seguem o mesmo formato:

```json
{
  "timestamp": "2026-10-15T21:45:00Z",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos",
  "caminho": "/produtos",
  "detalhes": {
    "nome": "O nome é obrigatório",
    "valor": "O valor deve ser maior que zero"
  }
}
```

O campo `detalhes` aparece apenas em erros de validação.

| Status | Quando ocorre |
|---|---|
| 400 | Campos inválidos, JSON malformado, id não numérico |
| 404 | Produto ou rota inexistente |
| 405 | Método HTTP não permitido |
| 406 | Formato de resposta não suportado |
| 409 | Violação de integridade no banco |
| 415 | Content-Type diferente de `application/json` |
| 500 | Erro inesperado (detalhes apenas no log do servidor) |

## Estrutura do projeto

```
src/main/java/com/riobranco/mouseomanos/produto_api/
├── ProdutoApiApplication.java     # Ponto de entrada
├── controller/
│   └── ProdutoController.java     # Endpoints REST
├── entity/
│   └── Produto.java               # Entidade JPA
├── repository/
│   └── ProdutoRepository.java     # Acesso a dados (Spring Data JPA)
└── exception/
    ├── ApiExceptionHandler.java   # Tratamento global de erros
    ├── ErroResposta.java          # Formato padrão de erro
    └── ProdutoNaoEncontradoException.java

src/main/resources/
├── application.properties         # Configuração (H2, console)
└── static/                        # Front-end (index.html, app.js, style.css)

src/test/java/...                  # Testes de integração
```

## Questões técnicas importantes

- **Banco em memória**: o H2 usa `jdbc:h2:mem:testdb`, então **todos os dados são perdidos ao reiniciar** a aplicação. Para uso real, é preciso configurar um banco persistente (por exemplo PostgreSQL ou MySQL) em `application.properties`.
- **Front-end e API na mesma origem**: o front-end estático fica em `src/main/resources/static` e é servido pelo próprio Spring Boot, por isso não há necessidade de configurar CORS.
- **Tratamento de erros centralizado**: as exceções são tratadas em um único `@RestControllerAdvice`, mantendo os controllers enxutos e as respostas de erro consistentes.
- **Validação via Bean Validation**: as regras ficam na entidade `Produto` e são aplicadas com `@Valid` nos endpoints de escrita.
- **Valor monetário como `Double`**: funciona para o estágio atual, mas `BigDecimal` é a escolha mais segura para dinheiro e deve ser adotado antes de cálculos de pedido e totais.
- **Entidade exposta diretamente**: a API recebe e devolve a entidade `Produto`, sem DTOs. Ao crescer o domínio (pedidos, itens), vale separar o modelo de API do modelo de persistência.
- **Sem autenticação**: os endpoints são públicos e o console do H2 está habilitado. Ambos devem ser protegidos ou desativados antes de qualquer uso em produção.

## Deploy

O workflow `.github/workflows/main_api-mousseomanos.yml` executa `mvn clean install` e publica o JAR em um Azure Web App a cada push na branch `main` (ou manualmente via `workflow_dispatch`). O deploy depende do secret de publish profile configurado no repositório.

## Fluxo de contribuição

- `main`: branch de produção; um push dispara o deploy automático.
- `raul-desenv`: branch de desenvolvimento. Envie as mudanças por aqui e abra um Pull Request para a `main`.
