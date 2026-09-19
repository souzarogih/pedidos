package br.com.pedidos.api.application.pedido;

import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;

import java.util.UUID;

public interface AdicionarItem {

    Pedido adicionar(UUID pedidoId, ItemPedido item);
}
