package br.com.pedidos.api.domain.pedido;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedidoTest {

    private static final ItemPedido CAFE_500 = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

    @Test
    void pedidoNovoNasceAbertoSemItensComIdentificador() {
        Pedido pedido = Pedido.novo();

        assertThat(pedido.id()).isNotNull();
        assertThat(pedido.status()).isEqualTo(StatusPedido.ABERTO);
        assertThat(pedido.itens()).isEmpty();
    }

    @Test
    void doisPedidosNovosTemIdentificadoresDiferentes() {
        Pedido primeiro = Pedido.novo();
        Pedido segundo = Pedido.novo();

        assertThat(primeiro.id()).isNotEqualTo(segundo.id());
    }

    @Test
    void adicionarItemDevolveOutroPedidoComOItem() {
        Pedido original = Pedido.novo();

        Pedido comItem = original.adicionarItem(CAFE_500);

        assertThat(comItem).isNotSameAs(original);
        assertThat(comItem.itens()).containsExactly(CAFE_500);
    }

    @Test
    void adicionarItemNaoAlteraOPedidoOriginal() {
        Pedido original = Pedido.novo();

        original.adicionarItem(CAFE_500);

        assertThat(original.itens()).isEmpty();
    }

    @Test
    void totalDeUmPedidoSemItensEhZero() {
        Pedido pedido = Pedido.novo();

        assertThat(pedido.total()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void totalDeUmPedidoComUmItemEhOSubtotalDoItem() {
        Pedido pedido = Pedido.novo().adicionarItem(CAFE_500);

        assertThat(pedido.total()).isEqualByComparingTo(new BigDecimal("37.80"));
    }

    @Test
    void totalDeUmPedidoComMultiplosItensEhASomaDosSubtotais() {
        ItemPedido outroItem = new ItemPedido("PAO-FRANCES", 5, new BigDecimal("1.20"));

        Pedido pedido = Pedido.novo()
                .adicionarItem(CAFE_500)
                .adicionarItem(outroItem);

        assertThat(pedido.total()).isEqualByComparingTo(new BigDecimal("43.80"));
    }

    @Test
    void adicionarItemEmPedidoPagoEhRecusado() {
        Pedido pago = Pedido.novo().pagar();

        assertThatThrownBy(() -> pago.adicionarItem(CAFE_500))
                .isInstanceOf(PedidoFechadoException.class);
    }

    @Test
    void adicionarItemEmPedidoCanceladoEhRecusado() {
        Pedido cancelado = Pedido.novo().cancelar();

        assertThatThrownBy(() -> cancelado.adicionarItem(CAFE_500))
                .isInstanceOf(PedidoFechadoException.class);
    }

    @Test
    void pagarUmPedidoAbertoRetornaPedidoPago() {
        Pedido pago = Pedido.novo().pagar();

        assertThat(pago.status()).isEqualTo(StatusPedido.PAGO);
    }

    @Test
    void cancelarUmPedidoAbertoRetornaPedidoCancelado() {
        Pedido cancelado = Pedido.novo().cancelar();

        assertThat(cancelado.status()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    void pagarUmPedidoJaPagoEhRecusado() {
        Pedido pago = Pedido.novo().pagar();

        assertThatThrownBy(pago::pagar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pagarUmPedidoCanceladoEhRecusado() {
        Pedido cancelado = Pedido.novo().cancelar();

        assertThatThrownBy(cancelado::pagar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelarUmPedidoJaPagoEhRecusado() {
        Pedido pago = Pedido.novo().pagar();

        assertThatThrownBy(pago::cancelar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelarUmPedidoJaCanceladoEhRecusado() {
        Pedido cancelado = Pedido.novo().cancelar();

        assertThatThrownBy(cancelado::cancelar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void listaDeItensDevolvidaNaoPodeSerModificada() {
        Pedido pedido = Pedido.novo().adicionarItem(CAFE_500);

        assertThatThrownBy(() -> pedido.itens().add(CAFE_500))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void modificarListaExternaAposConstruirNaoAfetaOPedido() {
        List<ItemPedido> itensExternos = new ArrayList<>(List.of(CAFE_500));
        Pedido pedido = new Pedido(java.util.UUID.randomUUID(), StatusPedido.ABERTO, itensExternos);

        itensExternos.add(new ItemPedido("PAO-FRANCES", 1, BigDecimal.ONE));

        assertThat(pedido.itens()).containsExactly(CAFE_500);
    }
}
