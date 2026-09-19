package br.com.pedidos.api.application.pedido;

public class PedidoSemItensException extends IllegalArgumentException {

    public PedidoSemItensException(String message) {
        super(message);
    }
}
