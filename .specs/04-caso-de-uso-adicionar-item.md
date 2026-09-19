# 04 — Caso de uso: Adicionar Item

## Contexto

`CriarPedido` (spec 02) e a persistência JPA + `POST /pedidos` (spec 03) já funcionam de ponta a ponta. Falta o segundo caso de uso: adicionar um item a um pedido já existente e aberto, via HTTP, persistindo a mudança. É a mesma forma de trabalho da spec 03 — porta de entrada, caso de uso, e agora reaproveitando a porta de saída `Pedidos` que já existe.

## Decisões a revisar antes de aprovar

1. **Exceção de "pedido fechado" no domínio**: hoje `Pedido.adicionarItem` lança `IllegalStateException` genérica quando o status não é `ABERTO`. Para o handler HTTP mapear isso para 409 sem um `catch` genérico de `IllegalStateException` (que capturaria qualquer `IllegalStateException` do sistema, não só essa regra), proponho criar `PedidoFechadoException extends IllegalStateException` no domínio e trocar o `throw` em `Pedido.adicionarItem`. Isso é o mesmo padrão já usado na spec 03 para `ItemInvalidoException`/`PedidoSemItensException`, e mantém compatibilidade: como a nova exceção estende `IllegalStateException`, os testes existentes que checam `IllegalStateException.class` continuam válidos (vou apertar as asserções para o tipo específico, por consistência). **Alternativa mais simples, sem tocar no domínio**: o handler captura `IllegalStateException` genérica → 409. Menos preciso, mas zero mudança em `Pedido.java`. Vou seguir com a opção 1 (exceção nomeada) por consistência com a spec 03, a menos que você prefira a alternativa.
2. **`clienteId` na resposta deste endpoint**: `PedidoResponse` tem `clienteId`, mas esse dado nunca é persistido (decisão da spec 03) e este endpoint não recebe `clienteId` na requisição (só o item). A resposta de `POST /pedidos/{id}/itens` vai reaproveitar `PedidoResponse` com `clienteId: null`. Se preferir, dá pra criar um DTO de resposta sem esse campo — mas isso duplica `PedidoResponse`/`ItemResponse` por uma diferença de um campo só. Vou seguir com `clienteId: null` a menos que você prefira o DTO separado.
3. **Como provar o 409 de ponta a ponta via HTTP**: não existe endpoint para pagar/cancelar um pedido ainda, então não há como fechar um pedido só por HTTP. Para o cenário HTTP desta spec, vou fechar um pedido manualmente via `UPDATE pedido SET status = 'PAGO' WHERE id = ...` direto no Postgres (rodando fora da aplicação) antes de chamar o endpoint — um artifício válido só para o teste manual, não algo que o sistema expõe. O 409 "de verdade" (via chamada de domínio, sem SQL manual) fica coberto pelo teste unitário do caso de uso.

## Contrato

**Porta de entrada** `AdicionarItem` (nova, `application.pedido`):
```java
public interface AdicionarItem {
    Pedido adicionar(UUID pedidoId, ItemPedido item);
}
```

**Caso de uso** `AdicionarItemService implements AdicionarItem`:
1. Busca o pedido pelo `Pedidos.buscarPorId(pedidoId)`. Se vazio, lança `PedidoNaoEncontradoException` (nova, `application.pedido`).
2. Chama `pedido.adicionarItem(item)` (domínio — já valida item e status, já recalcula o total por ser derivado).
3. Salva o resultado via `Pedidos.salvar(...)` e devolve o que a porta devolver.
4. Nenhum passo acima grava nada se o passo anterior falhar — `salvar` só é chamado se a busca e a adição no domínio derem certo.

**Endpoint**: `POST /pedidos/{id}/itens`, corpo = `ItemRequest` (já existe, reaproveitado). Resposta `200 OK` com `PedidoResponse` (`clienteId: null`, ver decisão 2).

**`PedidoController`**: ganha o novo método; `CriarPedido`/o método `criar` não mudam.

**`PedidoExceptionHandler`**: ganha dois `@ExceptionHandler` novos:
| Exceção | Status |
|---|---|
| `PedidoNaoEncontradoException` | 404 |
| `PedidoFechadoException` (ou `IllegalStateException`, ver decisão 1) | 409 |

`ItemInvalidoException` (item inválido) já está mapeada para 422 desde a spec 03 — reaproveitada sem mudança.

**`CasosDeUsoConfig`**: ganha `@Bean AdicionarItem adicionarItem(Pedidos pedidos)`.

## Regras

1. Buscar o pedido sempre pela porta `Pedidos`, nunca por acesso direto a JPA na camada de aplicação.
2. Pedido inexistente → `PedidoNaoEncontradoException` → 404.
3. Pedido não `ABERTO` (`PAGO`/`CANCELADO`) → recusado pelo domínio → 409.
4. Quantidade/preço inválidos → `ItemInvalidoException` (já existente) → 422.
5. Total do pedido é sempre recalculado a partir dos itens (já é assim desde a spec 01) — nenhuma soma manual no caso de uso.
6. Toda rejeição (404, 409, 422) acontece antes de qualquer `salvar`/`UPDATE` — nenhuma linha nova em `item_pedido`, nenhuma mudança em `pedido`.
7. `CriarPedido`/`CriarPedidoService`/o endpoint `POST /pedidos` não mudam.
8. Domínio e aplicação continuam sem import de Spring/JPA/HTTP.

## Casos de teste

**Unitários — `AdicionarItemServiceTest`** (fake `Pedidos` em memória, mesmo estilo de `CriarPedidoServiceTest`):
1. Pedido inexistente → `PedidoNaoEncontradoException`; fake nunca recebe chamada de `salvar`.
2. Pedido `PAGO` → exceção de pedido fechado (409 na borda); fake nunca recebe `salvar`.
3. Pedido `CANCELADO` → mesma exceção; fake nunca recebe `salvar`.
4. Pedido `ABERTO` com item válido → devolve `Pedido` com o item novo, total recalculado, mesmo UUID; fake recebe exatamente esse pedido em `salvar`.
5. O caso de uso devolve exatamente o que `Pedidos.salvar` devolver (mesmo padrão de `servicoDevolveExatamenteOQuePedidosSalvarDevolve` da spec 02).

**Unitários — ajuste em `PedidoTest`** (já existente): `adicionarItemEmPedidoPagoEhRecusado`/`CanceladoEhRecusado` passam a checar `PedidoFechadoException` (se a decisão 1 for aprovada como está).

**Cenário HTTP (manual, via `curl`/`psql`, aplicação + Postgres no ar)**:
1. Reaproveitar (ou recriar) o pedido CAFE-500 (2×18.90, total 37.80) da spec 03. `POST /pedidos/{id}/itens` com CAFE-500, quantidade 1, preço 18.90 → `200`, mesmo UUID, total `56.70`.
2. `POST /pedidos/<uuid-aleatorio-inexistente>/itens` com item válido → `404`.
3. No pedido do passo 1, `POST /pedidos/{id}/itens` com quantidade `0` → `422`; contagem de linhas em `item_pedido` para esse pedido não muda.
4. Fechar manualmente o pedido do passo 1 via SQL (`UPDATE pedido SET status = 'PAGO'`), tentar `POST /pedidos/{id}/itens` com item válido → `409`; contagem de linhas em `item_pedido` não muda.

## Arquivos afetados (limite desta spec — plano detalhado vem depois, com OK separado)

Novos:
- `application/pedido/AdicionarItem.java`
- `application/pedido/AdicionarItemService.java`
- `application/pedido/PedidoNaoEncontradoException.java`
- `domain/pedido/PedidoFechadoException.java` (se decisão 1 for aprovada)
- `test/.../application/pedido/AdicionarItemServiceTest.java`

Modificados:
- `domain/pedido/Pedido.java` (troca do tipo de exceção lançado, se decisão 1 aprovada)
- `test/.../domain/pedido/PedidoTest.java` (duas asserções, se decisão 1 aprovada)
- `adapters/entrada/rest/PedidoController.java` (novo método)
- `adapters/entrada/rest/PedidoExceptionHandler.java` (dois handlers novos)
- `config/CasosDeUsoConfig.java` (novo bean)

Nenhum arquivo de `pom.xml`, `application.yml`, `infra/docker-compose.yml` é tocado. Nenhum endpoint além de `POST /pedidos/{id}/itens` é criado.

## Fora de escopo (explícito)

- Endpoint para pagar/cancelar pedido (por isso o artifício do `UPDATE` manual no cenário HTTP do 409).
- Tratamento de `{id}` malformado (não-UUID) no path — hoje cairia no comportamento padrão do Spring (400), não testado explicitamente aqui.
- Qualquer mudança em `CriarPedido`/`CriarPedidoService`/`POST /pedidos`.
- Commit.
