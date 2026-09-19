package br.com.pedidos.api.application.pedido;

import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;
import br.com.pedidos.api.domain.pedido.PedidoFechadoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdicionarItemServiceTest {

    private static final ItemPedido CAFE_500 = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));
    private static final ItemPedido ITEM_EXTRA = new ItemPedido("CAFE-500", 1, new BigDecimal("18.90"));

    @Test
    void pedidoInexistenteEhRecusado() {
        PedidosEmMemoria pedidos = new PedidosEmMemoria();
        AdicionarItemService service = new AdicionarItemService(pedidos);

        assertThatThrownBy(() -> service.adicionar(UUID.randomUUID(), ITEM_EXTRA))
                .isInstanceOf(PedidoNaoEncontradoException.class);
        assertThat(pedidos.salvos).isEmpty();
    }

    @Test
    void pedidoPagoEhRecusado() {
        Pedido pago = Pedido.novo().adicionarItem(CAFE_500).pagar();
        PedidosEmMemoria pedidos = new PedidosEmMemoria();
        pedidos.existentes.put(pago.id(), pago);
        AdicionarItemService service = new AdicionarItemService(pedidos);

        assertThatThrownBy(() -> service.adicionar(pago.id(), ITEM_EXTRA))
                .isInstanceOf(PedidoFechadoException.class);
        assertThat(pedidos.salvos).isEmpty();
    }

    @Test
    void pedidoCanceladoEhRecusado() {
        Pedido cancelado = Pedido.novo().adicionarItem(CAFE_500).cancelar();
        PedidosEmMemoria pedidos = new PedidosEmMemoria();
        pedidos.existentes.put(cancelado.id(), cancelado);
        AdicionarItemService service = new AdicionarItemService(pedidos);

        assertThatThrownBy(() -> service.adicionar(cancelado.id(), ITEM_EXTRA))
                .isInstanceOf(PedidoFechadoException.class);
        assertThat(pedidos.salvos).isEmpty();
    }

    @Test
    void adicionarItemEmPedidoAbertoAtualizaTotalEMantemUuid() {
        Pedido aberto = Pedido.novo().adicionarItem(CAFE_500);
        PedidosEmMemoria pedidos = new PedidosEmMemoria();
        pedidos.existentes.put(aberto.id(), aberto);
        AdicionarItemService service = new AdicionarItemService(pedidos);

        Pedido resultado = service.adicionar(aberto.id(), ITEM_EXTRA);

        assertThat(resultado.id()).isEqualTo(aberto.id());
        assertThat(resultado.itens()).containsExactly(CAFE_500, ITEM_EXTRA);
        assertThat(resultado.total()).isEqualByComparingTo(new BigDecimal("56.70"));
    }

    @Test
    void servicoDevolveExatamenteOQuePedidosSalvarDevolve() {
        Pedido aberto = Pedido.novo().adicionarItem(CAFE_500);
        PedidosEmMemoria pedidos = new PedidosEmMemoria();
        pedidos.existentes.put(aberto.id(), aberto);
        Pedido pedidoDevolvidoPelaPorta = Pedido.novo();
        pedidos.proximoRetorno = pedidoDevolvidoPelaPorta;
        AdicionarItemService service = new AdicionarItemService(pedidos);

        Pedido resultado = service.adicionar(aberto.id(), ITEM_EXTRA);

        assertThat(resultado).isSameAs(pedidoDevolvidoPelaPorta);
    }

    private static final class PedidosEmMemoria implements Pedidos {

        private final List<Pedido> salvos = new ArrayList<>();
        private final java.util.Map<UUID, Pedido> existentes = new java.util.HashMap<>();
        private Pedido proximoRetorno;

        @Override
        public Pedido salvar(Pedido pedido) {
            salvos.add(pedido);
            existentes.put(pedido.id(), pedido);
            return proximoRetorno != null ? proximoRetorno : pedido;
        }

        @Override
        public Optional<Pedido> buscarPorId(UUID id) {
            return Optional.ofNullable(existentes.get(id));
        }
    }
}
