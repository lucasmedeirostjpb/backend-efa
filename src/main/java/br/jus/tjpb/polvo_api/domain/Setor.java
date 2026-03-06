package br.jus.tjpb.polvo_api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "efa_setores")
@Getter
@Setter
@NoArgsConstructor
public class Setor extends DomainEntityAuditableUpdate {

    private String sigla;
    private String nome;

    @Override
    public void setId(Long id) {
        super.setId(id);
    }

    public Setor(String sigla, String nome) {
        this.sigla = sigla;
        this.nome = nome;
    }
}
