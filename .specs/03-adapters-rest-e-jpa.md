# 03 — Adapters: banco, persistência JPA e HTTP

## Contexto

Domínio (spec 01) e aplicação (spec 02) estão prontos e isolados de framework. Falta a camada externa (adapters) que faz o serviço existir de verdade: um banco de dados, uma forma de persistir o `Pedido` nele, e uma forma de expor isso por HTTP. Este trabalho é dividido em três provas sequenciais, cada uma só avança depois que a anterior está comprovada.

**Nota de convenção de pacotes**: a partir desta revisão, a camada externa usa `br.com.pedidos.api.adapters.saida.*` (adapters que o núcleo chama, ex.: persistência) e `br.com.pedidos.api.adapters.entrada.*` (adapters que chamam o núcleo, ex.: REST). Isso substitui o nome de pacote `infra.pedido.jpa` cogitado numa versão anterior desta spec para o checkpoint 4B — nada foi implementado com o nome antigo, então não há renomeação de código, só do plano.

## Tarefa — três provas (checkpoints)

### 4A — Banco disponível ✅ já satisfeito

Postgres 16 sobe via `infra/docker-compose.yml`, `pg_isready` responde, `SELECT 1` executa. Feito em etapa anterior.

### 4B — Persistência pelo adapter (ainda não implementado — incluído no plano desta rodada)

Um `Pedido` criado no domínio é salvo de verdade no Postgres e recuperado de volta, sem que domínio ou aplicação saibam que JPA existe.

### 4C — HTTP (esta etapa)

Expor `CriarPedido` por `POST /pedidos`, sem o controller conhecer JPA. Detalhado abaixo.

## Decisões herdadas e novas

- **Cliente continua fora do domínio** (spec 02). O contrato HTTP desta etapa exige `clienteId` no corpo da requisição e da resposta — resolvido como **campo só de DTO**: `PedidoController` recebe `clienteId` no `PedidoRequest` e o devolve no `PedidoResponse` por simples eco, sem passar por `Pedido` (domínio) nem ser persistido. Depois do restart da aplicação, a linha e os itens continuam no banco — `clienteId` não, porque nunca esteve lá. Isso é explícito e intencional, não um bug.
- `ItemPedido.produto()` (domínio) mapeia para a coluna/campo `sku` (JPA e HTTP) — nomes diferentes de propósito, os adapters fazem a tradução.
- **Exceções nomeadas para mapear HTTP**: `ItemPedido` (domínio) passa a lançar `ItemInvalidoException` (em vez de `IllegalArgumentException` genérica) para quantidade/preço não positivos; `CriarPedidoService` (aplicação) passa a lançar `PedidoSemItensException` para lista nula/vazia. Ambas as exceções estendem `IllegalArgumentException` (mantém compatibilidade com o que os testes já verificavam) e são Java puro — nenhum import de Spring nelas. Isso é o que permite ao `PedidoExceptionHandler` (adapter) distinguir "regra de negócio violada" (422) de "formato/presença inválidos" (400) sem o domínio saber o que é HTTP.

## Contrato do checkpoint 4B

Pacote: `br.com.pedidos.api.adapters.saida.jpa`.

- `PedidoJpaEntity` — `@Entity` tabela `pedido`. Campos: `id` (`UUID`, `@Id`, sem `@GeneratedValue`), `status` (`StatusPedido`, `@Enumerated(STRING)`), `itens` (`@OneToMany`, cascade total, unidirecional via `@JoinColumn`). Sem coluna `total`.
- `ItemJpaEntity` — `@Entity` tabela `item_pedido`. Campos: `id` (chave técnica só da tabela, `Long` auto-gerado), `sku`, `quantidade`, `precoUnitario`.
- `PedidoJpaRepository` — `interface extends JpaRepository<PedidoJpaEntity, UUID>`.
- `PedidoJpaMapper` — conversão manual domínio ↔ entidade (sem MapStruct).
- `PedidosJpaAdapter` — `@Repository`, implementa `Pedidos`. `salvar`/`buscarPorId` são `@Transactional` (mapeia itens antes de fechar a sessão; `open-in-view=false`).

**Extensão de porta**: `Pedidos` ganha `Optional<Pedido> buscarPorId(UUID id)`.

`application.yml` configura `spring.datasource` para o Postgres local (credenciais `pedidos`/`pedidos`/`pedidos`, mesmas do compose) e `spring.jpa.open-in-view: false`.

## Contrato do checkpoint 4C

Pacotes:
- `br.com.pedidos.api.adapters.entrada.rest` — `PedidoController`, `PedidoRequest`, `ItemRequest`, `PedidoResponse`, `ItemResponse`, `PedidoExceptionHandler`, `ErroResponse`.
- `br.com.pedidos.api.config` — `CasosDeUsoConfig` (`@Configuration`): único lugar que sabe montar `new CriarPedidoService(pedidosJpaAdapter)` e expor como bean `CriarPedido`, já que a aplicação não tem `@Service`.

**Endpoint**: `POST /pedidos`.

Request (`PedidoRequest`):
```json
{
  "clienteId": "c-1",
  "itens": [
    { "sku": "CAFE-500", "quantidade": 2, "precoUnitario": 18.90 }
  ]
}
```

Response (`201 Created`, `PedidoResponse`):
```json
{
  "id": "<uuid>",
  "clienteId": "c-1",
  "itens": [ { "sku": "CAFE-500", "quantidade": 2, "precoUnitario": 18.90 } ],
  "status": "ABERTO",
  "total": 37.80
}
```

`precoUnitario` e `total` são números JSON (decimais), nunca string.

**`@Valid` cobre só formato e presença** — nunca a regra de negócio:
- `clienteId`: `@NotBlank`.
- `itens`: `@NotNull` (a **ausência** do campo é 400). **Sem** `@NotEmpty` — uma lista presente e vazia (`[]`) não é erro de formato, é erro de negócio (`PedidoSemItensException`, 422). `@Valid` cascateia para cada item.
- `ItemRequest.sku`: `@NotBlank`. `quantidade`/`precoUnitario`: tipos wrapper (`Integer`/`BigDecimal`, nunca `int` primitivo) com `@NotNull` — **sem** `@Positive`/`@DecimalMin`, porque quantidade/preço zero ou negativo é **presente mas inválido**, e essa regra mora no domínio (`ItemInvalidoException`, 422), não na validação de formato.

**`PedidoExceptionHandler`** (`@RestControllerAdvice`):
| Exceção | Status | Quando |
|---|---|---|
| `ItemInvalidoException` | 422 | quantidade ou preço não positivos (lançada por `ItemPedido`, domínio) |
| `PedidoSemItensException` | 422 | lista de itens presente e vazia (lançada por `CriarPedidoService`, aplicação) |
| `MethodArgumentNotValidException` | 400 | campo obrigatório ausente/formato inválido (`@Valid`) |
| `HttpMessageNotReadableException` | 400 | JSON malformado |

Corpo de erro simples: `{"mensagem": "..."}`.

**Fora do escopo desta etapa**: qualquer outro endpoint (sem `GET /pedidos/{id}`); a verificação de "o dado sobrevive a um restart" é feita consultando o Postgres diretamente (`psql`), não por HTTP.

## Regras (4B + 4C)

1. Domínio e aplicação continuam sem nenhum import de Spring/JPA/HTTP.
2. O ID do `Pedido` é o UUID gerado pelo domínio; a tabela nunca gera outro identificador.
3. Não existe coluna `total`; é sempre `Pedido.total()` recalculado.
4. Cada linha de `item_pedido` guarda `sku`, `quantidade`, `precoUnitario`.
5. Leitura de itens de uma entidade acontece dentro de transação, antes de virar `Pedido`.
6. `open-in-view` é `false`.
7. Testes unitários continuam passando sem banco.
8. `PedidosJpaAdapterIT` e o teste de contexto (renomeado `ApiApplicationIT`) só rodam contra Postgres real, por execução explícita.
9. `PedidoController` depende só de `CriarPedido` (porta) — nunca de `PedidosJpaAdapter`, `PedidoJpaRepository` ou qualquer tipo JPA.
10. `clienteId` nunca chega ao domínio nem à tabela — só existe em `PedidoRequest`/`PedidoResponse`.
11. Toda rejeição (400 ou 422) acontece antes de qualquer `INSERT` — nenhuma linha é gravada numa requisição recusada.
12. Nenhum H2, Lombok, MapStruct, ou endpoint além de `POST /pedidos`.

## Definição de pronto

1. `pom.xml` já tem `spring-boot-starter-data-jpa` e `org.postgresql:postgresql` — não duplicar.
2. `./mvnw test` com Postgres **parado**: só os `*Test.java` rodam, todos passam, sem tentar conexão de banco.
3. Com Postgres no ar: `./mvnw test -Dtest=ApiApplicationIT,PedidosJpaAdapterIT` passa.
4. `PedidosJpaAdapterIT`: salvar item CAFE-500 (2 × 18.90) e recuperar por UUID em chamada separada devolve status `ABERTO`, os mesmos itens, total `37.80`.
5. Aplicação rodando, `POST /pedidos` com `clienteId` + item CAFE-500 (2 × 18.90) válido → `201`, corpo com `id` (UUID), `clienteId` ecoado, item, `status: "ABERTO"`, `total: 37.80` (número).
6. `POST /pedidos` com item de `quantidade: 0` → `422`, e a contagem de linhas em `pedido` não muda.
7. `POST /pedidos` com `itens: []` → `422`, contagem de linhas não muda.
8. `POST /pedidos` com corpo JSON malformado → `400`, contagem de linhas não muda.
9. Consulta via `psql` ao pedido criado no passo 5 (por UUID) mostra a linha em `pedido` e as linhas correspondentes em `item_pedido`.
10. Reiniciar o processo da aplicação e repetir a mesma consulta `psql` mostra exatamente a mesma linha/itens — persistência sobrevive ao restart da aplicação (o dado está no Postgres, não em memória).
11. Nenhuma classe em `domain`/`application` importa `org.springframework.*`, `jakarta.persistence.*` ou `jakarta.servlet.*`/`jakarta.validation.*` fora dos DTOs de `adapters.entrada.rest` (inspeção).
12. Só o processo da aplicação iniciado para este teste é encerrado ao final — Postgres e qualquer outro container seguem como estavam.

## Fora de escopo (explícito)

- Qualquer endpoint além de `POST /pedidos`.
- H2, Lombok, MapStruct.
- Migração de schema (Flyway/Liquibase).
- Cliente como conceito de domínio (fica só em DTO).
- Commit.
