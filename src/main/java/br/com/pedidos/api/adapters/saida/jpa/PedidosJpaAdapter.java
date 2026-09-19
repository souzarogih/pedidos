package br.com.pedidos.api.adapters.saida.jpa;

import br.com.pedidos.api.application.pedido.Pedidos;
import br.com.pedidos.api.domain.pedido.Pedido;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PedidosJpaAdapter implements Pedidos {

    private final PedidoJpaRepository repository;

    public PedidosJpaAdapter(PedidoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Pedido salvar(Pedido pedido) {
        PedidoJpaEntity entity = PedidoJpaMapper.paraEntidade(pedido);
        PedidoJpaEntity salvo = repository.save(entity);
        return PedidoJpaMapper.paraDominio(salvo);
    }

    @Override
    @Transactional
    public Optional<Pedido> buscarPorId(UUID id) {
        return repository.findById(id).map(PedidoJpaMapper::paraDominio);
    }
}
