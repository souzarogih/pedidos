package br.com.pedidos.api.adapters.saida.jpa;

import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;
import br.com.pedidos.api.domain.pedido.StatusPedido;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PedidosJpaAdapterIT {

    @Autowired
    private PedidosJpaAdapter pedidosJpaAdapter;

    @Autowired
    private PedidoJpaRepository pedidoJpaRepository;

    private UUID idCriado;

    @AfterEach
    void limpar() {
        if (idCriado != null) {
            pedidoJpaRepository.deleteById(idCriado);
        }
    }

    @Test
    void salvaPedidoERecuperaPorUuidEmNovaTransacao() {
        ItemPedido cafe500 = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));
        Pedido pedido = Pedido.novo().adicionarItem(cafe500);

        Pedido salvo = pedidosJpaAdapter.salvar(pedido);
        idCriado = salvo.id();

        Optional<Pedido> recuperado = pedidosJpaAdapter.buscarPorId(salvo.id());

        assertThat(recuperado).isPresent();
        Pedido pedidoRecuperado = recuperado.get();
        assertThat(pedidoRecuperado.id()).isEqualTo(pedido.id());
        assertThat(pedidoRecuperado.status()).isEqualTo(StatusPedido.ABERTO);
        assertThat(pedidoRecuperado.itens()).containsExactly(cafe500);
        assertThat(pedidoRecuperado.total()).isEqualByComparingTo(new BigDecimal("37.80"));
    }
}
