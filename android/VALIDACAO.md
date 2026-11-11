# Validação da entrega Android

- Compilação `assembleDebug`: passou, APK gerado.
- `testDebugUnitTest`: 14 testes passaram (10 do ViewModel, 4 de HTTP/Repository).
- `lintDebug`: passou, zero erros. Há avisos de recursos de texto, versões de dependências,
  estilo/autofill e ícone. As versões de Lifecycle seguem o roteiro da aula.
- Azure: GET `/produtos`, `/cardapio` e `/pedidos` responderam HTTP 200 no domínio fornecido.
- CRUD real no Azure: POST de produto temporário, GET comprovando cadastro, PUT alterando valor,
  GET comprovando edição, DELETE e GET comprovando remoção passaram. O registro temporário foi removido;
  nenhum produto anterior foi alterado. Isso valida o servidor, não substitui a demonstração pelo app.
- Testes de tela: escritos em `TelaCrudTest`, mas ainda não concluídos. O emulador existente
  travou e a tentativa de reinicialização foi recusada por falta de espaço livre em disco.
  Não há evidência de execução desses dois testes nem de inspeção visual no aparelho.
- A rotação/recriação da Activity precisa ser demonstrada no aparelho da squad.
- A apostila de leitura de apoio mencionada no enunciado não estava disponível.

O back-end existente não foi alterado. A branch do Android não é a `main`, portanto seu push
não aciona o workflow de deploy da API.
