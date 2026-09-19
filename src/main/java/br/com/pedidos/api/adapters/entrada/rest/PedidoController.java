package br.com.pedidos.api.adapters.entrada.rest;

import br.com.pedidos.api.application.pedido.AdicionarItem;
import br.com.pedidos.api.application.pedido.CriarPedido;
import br.com.pedidos.api.domain.pedido.ItemPedido;
import br.com.pedidos.api.domain.pedido.Pedido;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final CriarPedido criarPedido;
    private final AdicionarItem adicionarItem;

    public PedidoController(CriarPedido criarPedido, AdicionarItem adicionarItem) {
        this.criarPedido = criarPedido;
        this.adicionarItem = adicionarItem;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest request) {
        List<ItemPedido> itens = request.itens().stream()
                .map(item -> new ItemPedido(item.sku(), item.quantidade(), item.precoUnitario()))
                .toList();

        Pedido pedido = criarPedido.criar(itens);

        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.de(pedido, request.clienteId()));
    }

    @PostMapping("/{id}/itens")
    public ResponseEntity<PedidoResponse> adicionarItem(@PathVariable UUID id, @Valid @RequestBody ItemRequest request) {
        ItemPedido item = new ItemPedido(request.sku(), request.quantidade(), request.precoUnitario());

        Pedido pedido = adicionarItem.adicionar(id, item);

        return ResponseEntity.ok(PedidoResponse.de(pedido, null));
    }
}
