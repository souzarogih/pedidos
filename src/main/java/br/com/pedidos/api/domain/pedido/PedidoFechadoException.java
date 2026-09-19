package br.com.pedidos.api.domain.pedido;

public class PedidoFechadoException extends IllegalStateException {

    public PedidoFechadoException(String message) {
        super(message);
    }
}
