package br.com.pedidos.api.domain.pedido;

public class ItemInvalidoException extends IllegalArgumentException {

    public ItemInvalidoException(String message) {
        super(message);
    }
}
