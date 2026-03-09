package br.jus.tjpb.polvo_api.boundaries.api.dto;

import br.jus.tjpb.polvo_api.domain.StatusMeta;
import br.jus.tjpb.polvo_api.domain.NivelDificuldade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MetaRequestDTO {

    @NotBlank(message = "O título é obrigatório.")
    private String titulo;

    private String descricao;

    private Long eixoId;

    private Long setorId;

    private String eixoNome;

    private String setorNome;

    private String artigo;

    @NotNull(message = "O ano do ciclo é obrigatório.")
    private Integer anoCiclo;

    private LocalDate deadline;

    @NotNull(message = "O status é obrigatório.")
    private StatusMeta status;

    private NivelDificuldade nivelDificuldade;

    private String evidenciasAuditoria;

    private String observacoes;

    @NotNull(message = "O percentual máximo é obrigatório.")
    @PositiveOrZero(message = "O percentual máximo deve ser maior ou igual a zero.")
    @com.fasterxml.jackson.annotation.JsonProperty("pMaximo")
    private BigDecimal pMaximo;

    @PositiveOrZero(message = "A estimativa real deve ser maior ou igual a zero.")
    private BigDecimal estimativaReal;

    @PositiveOrZero(message = "O teto estimado deve ser maior ou igual a zero.")
    private BigDecimal tetoEstimado;

    @PositiveOrZero(message = "Os pontos atingidos devem ser maiores ou iguais a zero.")
    private BigDecimal pontosAtingidos;
}
