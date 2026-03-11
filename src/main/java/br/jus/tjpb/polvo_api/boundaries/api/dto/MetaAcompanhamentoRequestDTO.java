package br.jus.tjpb.polvo_api.boundaries.api.dto;

import br.jus.tjpb.polvo_api.domain.NivelDificuldade;
import br.jus.tjpb.polvo_api.domain.StatusMeta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MetaAcompanhamentoRequestDTO {

    @NotNull(message = "O status é obrigatório.")
    private StatusMeta status;

    private NivelDificuldade nivelDificuldade;

    private String evidenciasAuditoria;

    private String observacoes;

    @PositiveOrZero(message = "A estimativa real deve ser maior ou igual a zero.")
    private BigDecimal estimativaReal;

    @PositiveOrZero(message = "O teto estimado deve ser maior ou igual a zero.")
    private BigDecimal tetoEstimado;

    @PositiveOrZero(message = "Os pontos atingidos devem ser maiores ou iguais a zero.")
    private BigDecimal pontosAtingidos;
}