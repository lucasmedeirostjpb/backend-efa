package br.jus.tjpb.polvo_api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "metas")
@Getter
@Setter
@NoArgsConstructor
public class Meta extends DomainEntityAuditableUpdate {

    private String titulo;
    private String descricao;
    private Boolean concluida = false;

    public Meta(String titulo, String descricao) {
        this.titulo = titulo;
        this.descricao = descricao;
    }
}
