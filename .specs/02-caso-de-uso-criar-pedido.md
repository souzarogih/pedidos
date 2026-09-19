# 02 — Caso de uso: Criar Pedido

## Contexto

O domínio de `Pedido` já existe (spec 01), isolado de qualquer framework. Esta spec adiciona a primeira fatia da camada de aplicação: o caso de uso que orquestra a criação de um pedido e o persiste através de uma porta, sem a aplicação conhecer Spring, JPA, HTTP ou qualquer adapter concreto.

**Decisão registrada**: o pedido original desta spec incluía "cliente" como entrada do caso de uso. Como o domínio `Pedido` (spec 01) não modela cliente, e estender o domínio para isso está fora do escopo combinado agora, **cliente fica de fora desta spec**. `CriarPedido` recebe apenas os itens. Cliente será tratado em uma spec futura, quando o conceito de Cliente existir de fato no domínio.

## Tarefa

Modelar o caso de uso "criar pedido" como três peças na camada de aplicação:

- **Porta de entrada** `CriarPedido`: interface que descreve o que se pode pedir ao núcleo — criar um pedido a partir de uma lista de itens.
- **Caso de uso** `CriarPedidoService`: implementa `CriarPedido`. Valida a lista de itens, monta o `Pedido` usando só a API do domínio (`Pedido.novo()` + `adicionarItem`), e pede para salvar através da porta de saída.
- **Porta de saída** `Pedidos`: interface que descreve o que o caso de uso precisa de fora — salvar um pedido e receber de volta o pedido salvo.

Assinaturas propostas (a confirmar no plano):

```java
public interface CriarPedido {
    Pedido criar(List<ItemPedido> itens);
}

public interface Pedidos {
    Pedido salvar(Pedido pedido);
}

public class CriarPedidoService implements CriarPedido {
    public CriarPedidoService(Pedidos pedidos) { ... }
    public Pedido criar(List<ItemPedido> itens) { ... }
}
```

## Regras

1. **Lista de itens vazia (ou nula) é recusada**: `criar` não aceita uma lista sem nenhum item — é um erro, não um pedido vazio salvo.
2. **O domínio é construído só com a API do domínio**: o caso de uso não monta um `Pedido` "por fora" (sem usar `Pedido.novo()`/`adicionarItem`) — nenhuma regra de negócio é duplicada na aplicação.
3. **Persistência é sempre pela porta**: o caso de uso nunca sabe como o pedido é salvo — só chama `Pedidos.salvar(pedido)` e devolve o que a porta devolver.
4. **A aplicação não importa infraestrutura**: nenhuma classe em `application.pedido` importa `org.springframework.*`, `jakarta.persistence.*`, `jakarta.servlet.*`/HTTP, ou qualquer adapter concreto. Só domínio e `java.*`.
5. **Sem DTOs**: a porta de entrada recebe e devolve tipos do domínio (`ItemPedido`, `Pedido`) diretamente — nenhuma classe nova só para "carregar dados".

## Definição de pronto

1. Chamar `criar` com uma lista vazia lança erro.
2. Chamar `criar` com uma lista nula lança erro.
3. Chamar `criar` com uma lista com um ou mais itens válidos produz um `Pedido` contendo exatamente esses itens.
4. O pedido devolvido por `criar` é o mesmo objeto devolvido por `Pedidos.salvar` (o caso de uso não decide o resultado final por conta própria depois de salvar).
5. Uma implementação em memória de `Pedidos` (uma lista/mapa Java, dentro do próprio teste) é suficiente para testar `CriarPedidoService` de ponta a ponta — sem Spring, sem banco.
6. Nenhum import de `org.springframework.*`, `jakarta.persistence.*` ou HTTP aparece em `application.pedido` (verificação por inspeção).

## Fora de escopo (explícito)

- Cliente (adiado, ver decisão acima).
- Controller, JPA, DTOs, configuração Spring — nada disso é criado nesta spec.
- Commit — implementação fica no working tree.
