package br.com.pedidos.api.adapters.entrada.rest;

import java.math.BigDecimal;

public record ItemResponse(String sku, int quantidade, BigDecimal precoUnitario) {
}
