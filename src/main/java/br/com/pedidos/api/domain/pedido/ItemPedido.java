package br.com.pedidos.api.domain.pedido;

import java.math.BigDecimal;
import java.util.Objects;

public record ItemPedido(String produto, int quantidade, BigDecimal precoUnitario) {

    public ItemPedido {
        Objects.requireNonNull(produto, "produto não pode ser nulo");
        Objects.requireNonNull(precoUnitario, "precoUnitario não pode ser nulo");
        if (quantidade <= 0) {
            throw new ItemInvalidoException("quantidade deve ser positiva");
        }
        if (precoUnitario.signum() <= 0) {
            throw new ItemInvalidoException("precoUnitario deve ser positivo");
        }
    }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
