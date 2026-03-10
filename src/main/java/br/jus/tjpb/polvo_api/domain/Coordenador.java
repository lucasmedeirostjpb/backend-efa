package br.jus.tjpb.polvo_api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "efa_coordenadores")
@Getter
@Setter
@NoArgsConstructor
public class Coordenador extends DomainEntityAuditableUpdate {

    private String nome;
    private String email;
    private String loginKeycloak;

    @Override
    public void setId(Long id) {
        super.setId(id);
    }

    public Coordenador(String nome) {
        this.nome = nome;
    }
}
