package br.com.pedidos.api.domain.pedido;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemPedidoTest {

    @Test
    void quantidadeZeroEhRecusada() {
        assertThatThrownBy(() -> new ItemPedido("CAFE-500", 0, new BigDecimal("18.90")))
                .isInstanceOf(ItemInvalidoException.class);
    }

    @Test
    void quantidadeNegativaEhRecusada() {
        assertThatThrownBy(() -> new ItemPedido("CAFE-500", -1, new BigDecimal("18.90")))
                .isInstanceOf(ItemInvalidoException.class);
    }

    @Test
    void precoUnitarioZeroEhRecusado() {
        assertThatThrownBy(() -> new ItemPedido("CAFE-500", 2, BigDecimal.ZERO))
                .isInstanceOf(ItemInvalidoException.class);
    }

    @Test
    void precoUnitarioNegativoEhRecusado() {
        assertThatThrownBy(() -> new ItemPedido("CAFE-500", 2, new BigDecimal("-1")))
                .isInstanceOf(ItemInvalidoException.class);
    }

    @Test
    void subtotalEhQuantidadeVezesPrecoUnitarioComoBigDecimal() {
        ItemPedido item = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

        assertThat(item.subtotal()).isEqualByComparingTo(new BigDecimal("37.80"));
    }
}
