package br.com.pedidos.api.adapters.saida.jpa;

import br.com.pedidos.api.domain.pedido.StatusPedido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedido")
public class PedidoJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private StatusPedido status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private List<ItemJpaEntity> itens = new ArrayList<>();

    protected PedidoJpaEntity() {
    }

    public PedidoJpaEntity(UUID id, StatusPedido status, List<ItemJpaEntity> itens) {
        this.id = id;
        this.status = status;
        this.itens = itens;
    }

    public UUID getId() {
        return id;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public List<ItemJpaEntity> getItens() {
        return itens;
    }
}
