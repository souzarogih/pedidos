package br.com.pedidos.api.config;

import br.com.pedidos.api.application.pedido.AdicionarItem;
import br.com.pedidos.api.application.pedido.AdicionarItemService;
import br.com.pedidos.api.application.pedido.CriarPedido;
import br.com.pedidos.api.application.pedido.CriarPedidoService;
import br.com.pedidos.api.application.pedido.Pedidos;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CasosDeUsoConfig {

    @Bean
    public CriarPedido criarPedido(Pedidos pedidos) {
        return new CriarPedidoService(pedidos);
    }

    @Bean
    public AdicionarItem adicionarItem(Pedidos pedidos) {
        return new AdicionarItemService(pedidos);
    }
}
