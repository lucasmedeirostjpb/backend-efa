package br.jus.tjpb.polvo_api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "efa_eixos_tematicos")
@Getter
@Setter
@NoArgsConstructor
public class EixoTematico extends DomainEntityAuditableUpdate {

    private String nome;

    @Override
    public void setId(Long id) {
        super.setId(id);
    }

    public EixoTematico(String nome) {
        this.nome = nome;
    }
}
