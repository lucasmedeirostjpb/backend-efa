package br.jus.tjpb.polvo_api.domain;

import java.util.Optional;

public interface CoordenadorRepository extends DomainEntityRepository<Coordenador> {
    Optional<Coordenador> findByLoginKeycloak(String loginKeycloak);

    Optional<Coordenador> findByNome(String nome);
}
