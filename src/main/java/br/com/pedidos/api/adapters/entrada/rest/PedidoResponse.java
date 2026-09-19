package br.com.pedidos.api.adapters.entrada.rest;

import br.com.pedidos.api.domain.pedido.Pedido;
import br.com.pedidos.api.domain.pedido.StatusPedido;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PedidoResponse(
        UUID id,
        String clienteId,
        List<ItemResponse> itens,
        StatusPedido status,
        BigDecimal total
) {

    public static PedidoResponse de(Pedido pedido, String clienteId) {
        List<ItemResponse> itens = pedido.itens().stream()
                .map(item -> new ItemResponse(item.produto(), item.quantidade(), item.precoUnitario()))
                .toList();
        return new PedidoResponse(pedido.id(), clienteId, itens, pedido.status(), pedido.total());
    }
}
