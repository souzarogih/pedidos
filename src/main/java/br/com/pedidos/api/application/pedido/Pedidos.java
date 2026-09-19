package br.com.pedidos.api.application.pedido;

import br.com.pedidos.api.domain.pedido.Pedido;

import java.util.Optional;
import java.util.UUID;

public interface Pedidos {

    Pedido salvar(Pedido pedido);

    Optional<Pedido> buscarPorId(UUID id);
}
