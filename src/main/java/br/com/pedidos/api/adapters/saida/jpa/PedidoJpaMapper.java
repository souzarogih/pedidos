package br.com.pedidos.api.adapters.saida.jpa;

import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;

import java.util.List;

public final class PedidoJpaMapper {

    private PedidoJpaMapper() {
    }

    public static PedidoJpaEntity paraEntidade(Pedido pedido) {
        List<ItemJpaEntity> itens = pedido.itens().stream()
                .map(PedidoJpaMapper::paraEntidadeItem)
                .toList();
        return new PedidoJpaEntity(pedido.id(), pedido.status(), itens);
    }

    public static Pedido paraDominio(PedidoJpaEntity entity) {
        List<ItemPedido> itens = entity.getItens().stream()
                .map(PedidoJpaMapper::paraDominioItem)
                .toList();
        return new Pedido(entity.getId(), entity.getStatus(), itens);
    }

    private static ItemJpaEntity paraEntidadeItem(ItemPedido item) {
        return new ItemJpaEntity(item.produto(), item.quantidade(), item.precoUnitario());
    }

    private static ItemPedido paraDominioItem(ItemJpaEntity entity) {
        return new ItemPedido(entity.getSku(), entity.getQuantidade(), entity.getPrecoUnitario());
    }
}
