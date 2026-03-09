package br.jus.tjpb.polvo_api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "efa_metas")
@Getter
@Setter
@NoArgsConstructor
public class Meta extends DomainEntityAuditableUpdate {

    private String titulo;
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eixo_id")
    private EixoTematico eixo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id")
    private Setor setor;

    private String artigo;
    private Integer anoCiclo;
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private StatusMeta status = StatusMeta.PENDENTE;

    @Enumerated(EnumType.STRING)
    private NivelDificuldade nivelDificuldade = NivelDificuldade.SEM_DIFICULDADES;

    @Column(columnDefinition = "TEXT")
    private String evidenciasAuditoria;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    private BigDecimal pMaximo;
    private BigDecimal estimativaReal;
    private BigDecimal tetoEstimado;
    private BigDecimal pontosAtingidos;

    public Meta(String titulo, String descricao) {
        this.titulo = titulo;
        this.descricao = descricao;
    }
}
