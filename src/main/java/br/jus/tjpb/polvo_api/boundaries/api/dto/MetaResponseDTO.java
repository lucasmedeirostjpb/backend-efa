package br.jus.tjpb.polvo_api.boundaries.api.dto;

import br.jus.tjpb.polvo_api.domain.StatusMeta;
import br.jus.tjpb.polvo_api.domain.NivelDificuldade;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MetaResponseDTO {
    private Long id;
    private String titulo;
    private String descricao;

    private Long eixoId;
    private String eixoNome;

    private Long setorId;
    private String setorSigla;
    private String setorNome;

    private String artigo;
    private Integer anoCiclo;
    private LocalDate deadline;

    private StatusMeta status;
    private NivelDificuldade nivelDificuldade;
    private String evidenciasAuditoria;
    private String observacoes;

    @com.fasterxml.jackson.annotation.JsonProperty("pMaximo")
    private BigDecimal pMaximo;
    private BigDecimal estimativaReal;
    private BigDecimal tetoEstimado;
    private BigDecimal pontosAtingidos;
}
