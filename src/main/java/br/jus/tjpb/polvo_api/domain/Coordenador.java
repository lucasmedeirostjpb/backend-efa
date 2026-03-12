package br.jus.tjpb.polvo_api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "efa_coordenadores")
@Getter
@Setter
@NoArgsConstructor
public class Coordenador extends DomainEntityAuditableUpdate {

    private String nome;
    private String email;
    private String loginKeycloak;

    @OneToMany(mappedBy = "coordenador", fetch = FetchType.LAZY)
    private List<Delegacao> delegacoes = new ArrayList<>();

    @Override
    public void setId(Long id) {
        super.setId(id);
    }

    public Coordenador(String nome) {
        this.nome = nome;
    }
}
