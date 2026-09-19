package br.com.pedidos.api.application.pedido;

import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriarPedidoServiceTest {

    private static final ItemPedido CAFE_500 = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

    @Test
    void criarComListaVaziaEhRecusado() {
        CriarPedidoService service = new CriarPedidoService(new PedidosEmMemoria());

        assertThatThrownBy(() -> service.criar(List.of()))
                .isInstanceOf(PedidoSemItensException.class);
    }

    @Test
    void criarComListaNulaEhRecusado() {
        CriarPedidoService service = new CriarPedidoService(new PedidosEmMemoria());

        assertThatThrownBy(() -> service.criar(null))
                .isInstanceOf(PedidoSemItensException.class);
    }

    @Test
    void criarComItensValidosProduzPedidoComEssesItens() {
        CriarPedidoService service = new CriarPedidoService(new PedidosEmMemoria());

        Pedido pedido = service.criar(List.of(CAFE_500));

        assertThat(pedido.itens()).containsExactly(CAFE_500);
    }

    @Test
    void pedidoCriadoEhPassadoParaAPortaDeSaida() {
        PedidosEmMemoria pedidos = new PedidosEmMemoria();
        CriarPedidoService service = new CriarPedidoService(pedidos);

        Pedido pedido = service.criar(List.of(CAFE_500));

        assertThat(pedidos.salvos).containsExactly(pedido);
    }

    @Test
    void servicoDevolveExatamenteOQuePedidosSalvarDevolve() {
        PedidosEmMemoria pedidos = new PedidosEmMemoria();
        Pedido pedidoDevolvidoPelaPorta = Pedido.novo();
        pedidos.proximoRetorno = pedidoDevolvidoPelaPorta;
        CriarPedidoService service = new CriarPedidoService(pedidos);

        Pedido resultado = service.criar(List.of(CAFE_500));

        assertThat(resultado).isSameAs(pedidoDevolvidoPelaPorta);
    }

    private static final class PedidosEmMemoria implements Pedidos {

        private final List<Pedido> salvos = new ArrayList<>();
        private Pedido proximoRetorno;

        @Override
        public Pedido salvar(Pedido pedido) {
            salvos.add(pedido);
            return proximoRetorno != null ? proximoRetorno : pedido;
        }

        @Override
        public Optional<Pedido> buscarPorId(UUID id) {
            return salvos.stream().filter(pedido -> pedido.id().equals(id)).findFirst();
        }
    }
}
