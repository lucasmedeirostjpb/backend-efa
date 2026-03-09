package br.jus.tjpb.polvo_api.domain;

import java.util.Optional;

public interface EixoTematicoRepository extends DomainEntityRepository<EixoTematico> {
    Optional<EixoTematico> findByNome(String nome);
}
