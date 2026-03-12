package br.jus.tjpb.polvo_api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "efa_delegacoes")
@Getter
@Setter
@NoArgsConstructor
public class Delegacao extends DomainEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coordenador_id", nullable = false)
    private Coordenador coordenador;

    private String delegadoEmail;
    private String delegadoNome;

    public Delegacao(Coordenador coordenador, String delegadoEmail, String delegadoNome) {
        this.coordenador = coordenador;
        this.delegadoEmail = delegadoEmail;
        this.delegadoNome = delegadoNome;
    }
}