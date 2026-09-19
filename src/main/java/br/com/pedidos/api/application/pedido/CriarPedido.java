package br.com.pedidos.api.application.pedido;

import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;

import java.util.List;

public interface CriarPedido {

    Pedido criar(List<ItemPedido> itens);
}
