package br.com.pedidos.api.application.pedido;

import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;

import java.util.UUID;

public class AdicionarItemService implements AdicionarItem {

    private final Pedidos pedidos;

    public AdicionarItemService(Pedidos pedidos) {
        this.pedidos = pedidos;
    }

    @Override
    public Pedido adicionar(UUID pedidoId, ItemPedido item) {
        Pedido pedido = pedidos.buscarPorId(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException("pedido não encontrado: " + pedidoId));

        Pedido atualizado = pedido.adicionarItem(item);

        return pedidos.salvar(atualizado);
    }
}
