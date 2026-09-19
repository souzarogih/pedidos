# 01 — Domínio de Pedido

## Contexto

Este é o primeiro recorte de domínio do serviço "pedidos". Antes de qualquer controller, repositório ou persistência, precisamos do núcleo de negócio — o que é um pedido, como ele nasce, como itens são adicionados e como ele muda de situação — modelado de forma isolada, sem nenhuma dependência de framework, conforme `AGENTS.md`.

## Tarefa

Modelar o domínio inicial de pedido: um pedido que nasce como rascunho, recebe itens de compra e pode ser pago ou cancelado. O conceito central é o comportamento, não a estrutura de dados — a estrutura é consequência do comportamento descrito abaixo.

Conceitos envolvidos (nomes de implementação sugeridos, não obrigatórios): **Pedido**, **ItemPedido**, **StatusPedido**.

## Regras

Descritas em linguagem de comportamento; a correspondência com nomes de implementação vem entre parênteses só como referência.

1. **Nascimento de um pedido**: um pedido novo é criado como um rascunho, sem itens, identificado de forma única e global (UUID), e começa na situação "aberto para receber itens". (`Pedido.novo()`)

2. **Adicionar um item é criar uma nova versão do pedido**: ao incluir um item de compra em um pedido, o resultado é um outro pedido — contendo o item a mais — e o pedido original permanece como estava, intocado. Nada no domínio muda um pedido "no lugar". (`adicionarItem` retorna novo `Pedido`)

3. **Só se adiciona item enquanto o pedido está aberto**: se o pedido já foi pago ou já foi cancelado, a tentativa de adicionar um item é recusada — não produz um novo pedido, produz um erro.

4. **Quantidade e preço de um item são sempre positivos**: um item de compra com quantidade zero, negativa, ou com preço unitário zero ou negativo não é um item válido — a tentativa de criá-lo é recusada, com erro, no próprio item (não apenas quando ele é adicionado a um pedido).

5. **Dinheiro nunca é aproximado**: qualquer valor monetário — preço unitário, subtotal de um item, total do pedido — é representado com `BigDecimal`. Nenhum `double`/`float` aparece em nenhum ponto do domínio.

6. **O total é sempre calculado, nunca guardado**: o pedido não guarda um total separado que possa ficar desatualizado; o total é derivado, a qualquer momento, a partir da soma dos subtotais (quantidade × preço unitário) dos itens atuais do pedido.

   *Exemplo concreto usado como referência de comportamento*: um item de "CAFE-500", quantidade 2, preço unitário 18.90 → subtotal 37.80. Um pedido só com esse item tem total 37.80.

7. **Situações e transições do pedido** (`StatusPedido`): um pedido aberto pode ser pago ou pode ser cancelado — essas são as únicas duas transições possíveis a partir de "aberto". Qualquer outra transição é recusada: um pedido pago não pode ser pago de novo, não pode ser cancelado; um pedido cancelado não pode ser pago, não pode ser cancelado de novo. Toda transição recusada é um erro, não um "não fez nada" silencioso.

8. **Listas não vazam para fora**: a lista de itens de um pedido não pode ser alterada por quem a recebe. Nem passando uma lista externa na construção (o pedido copia o que recebe), nem alterando a lista devolvida por um getter (a leitura devolve algo que não pode ser mutado). Cópia defensiva nos dois sentidos.

9. **Isolamento do domínio**: nenhuma classe descrita aqui depende de Spring, de JPA, ou de qualquer anotação/tipo de um desses frameworks. O domínio é Java puro.

10. **Forma preferida de implementação**: usar `record` para `Pedido` e `ItemPedido` sempre que a imutabilidade nativa do record for suficiente para cumprir as regras acima (cópia defensiva de listas ainda precisa ser feita explicitamente, já que `record` não faz isso sozinho).

## Definição de pronto

Cada regra acima vira pelo menos um caso de teste. A lista abaixo é o mínimo esperado — inclui os casos de erro, não só o caminho feliz.

1. `Pedido.novo()` cria um pedido com situação "aberto", zero itens, e um identificador (UUID) não nulo.
2. Duas chamadas a `Pedido.novo()` produzem pedidos com identificadores diferentes.
3. Adicionar um item válido a um pedido aberto retorna um objeto `Pedido` diferente (por identidade de objeto) do original, contendo o novo item.
4. Após adicionar um item, o pedido original continua com a mesma lista de itens que tinha antes (não foi alterado).
5. Criar um `ItemPedido` com quantidade zero é recusado com erro.
6. Criar um `ItemPedido` com quantidade negativa é recusado com erro.
7. Criar um `ItemPedido` com preço unitário zero é recusado com erro.
8. Criar um `ItemPedido` com preço unitário negativo é recusado com erro.
9. O total de um pedido sem itens é zero.
10. O total de um pedido com um item "CAFE-500", quantidade 2, preço unitário 18.90, é 37.80.
11. O total de um pedido com múltiplos itens é a soma dos subtotais de cada item.
12. Adicionar um item a um pedido pago é recusado com erro (não produz um novo pedido).
13. Adicionar um item a um pedido cancelado é recusado com erro.
14. Pagar um pedido aberto retorna um pedido na situação "pago".
15. Cancelar um pedido aberto retorna um pedido na situação "cancelado".
16. Pagar um pedido que já está pago é recusado com erro.
17. Pagar um pedido cancelado é recusado com erro.
18. Cancelar um pedido que já está pago é recusado com erro.
19. Cancelar um pedido que já está cancelado é recusado com erro.
20. A lista de itens devolvida por um getter não pode ser modificada diretamente (tentar `add`/`remove`/`clear` nela lança erro) e, mesmo que lançasse, não afetaria o pedido.
21. Construir um pedido/item a partir de uma lista externa e depois modificar essa lista externa não afeta o pedido já construído.
22. Nenhuma classe do domínio importa tipo de `org.springframework.*` ou `jakarta.persistence.*` (verificável por inspeção/teste arquitetural).
23. Todo campo monetário no domínio é `BigDecimal` — não há `double`/`float` em nenhuma classe do domínio.

## Ambiguidades a decidir antes de implementar

- **"c-1" no exemplo do enunciado**: o exemplo cita "c-1 compra CAFE-500", sugerindo um cliente ("c-1"), mas o domínio inicial listado (Pedido, ItemPedido, StatusPedido) não inclui conceito de Cliente. Pedido deve ter um campo de identificação do cliente (ex.: `clienteId`) nesta primeira fatia, ou isso fica para uma spec futura e "c-1" é só contexto da aula, sem virar campo agora?
- **Identidade e granularidade de `ItemPedido`**: "CAFE-500" parece ser um código de produto (SKU). O item deve carregar só uma `String` de SKU, ou um value object de produto mais rico (nome, categoria etc.)? E dois itens com o mesmo SKU adicionados separadamente devem se fundir (somar quantidade) ou conviver como duas linhas distintas no pedido?
- **Tipo de quantidade**: `int`/`long` (unidades inteiras) ou `BigDecimal` (permitindo quantidades fracionárias, ex. produtos por peso)? O exemplo (quantidade 2) é compatível com qualquer um dos dois.
- **Escala e arredondamento do dinheiro**: ao multiplicar quantidade × preço unitário, qual escala/`RoundingMode` o `BigDecimal` resultante deve ter? O exemplo (2 × 18.90 = 37.80) não força uma escolha porque não há arredondamento envolvido.
- **Pagar/cancelar um pedido sem itens**: é permitido pagar um pedido "aberto" que não tem nenhum item, ou isso deveria ser recusado (regra de negócio não mencionada explicitamente no pedido original)?
- **Retorno de `pagar`/`cancelar`**: o enunciado só afirma explicitamente que `adicionarItem` devolve um novo objeto. Presumi (regra 2/7 acima) que `pagar` e `cancelar` seguem o mesmo padrão de imutabilidade — confirmar se é essa a intenção, já que o texto não repete "devolve novo pedido" para essas duas operações.
- **`StatusPedido` como enum vs. record**: `AGENTS.md`/o pedido pedem uso de `record`, mas uma situação sem dados associados (ABERTO/PAGO/CANCELADO) é o caso clássico de `enum` em Java. Confirmar se `StatusPedido` deve ser um enum simples (minha leitura, mais idiomático) ou se "usar records" deveria se estender a ele de alguma forma (ex.: sealed interface de records vazios).
