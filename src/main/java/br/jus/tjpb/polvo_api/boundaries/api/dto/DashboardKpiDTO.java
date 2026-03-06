package br.jus.tjpb.polvo_api.boundaries.api.dto;

import java.math.BigDecimal;

public record DashboardKpiDTO(
        Long totalMetas,
        BigDecimal somaPontosAplicaveis,
        BigDecimal somaPontosAtingidos,
        BigDecimal percentualTracao) {
    public DashboardKpiDTO(Long totalMetas, BigDecimal somaPontosAplicaveis, BigDecimal somaPontosAtingidos) {
        this(totalMetas, somaPontosAplicaveis, somaPontosAtingidos, BigDecimal.ZERO);
    }
}
