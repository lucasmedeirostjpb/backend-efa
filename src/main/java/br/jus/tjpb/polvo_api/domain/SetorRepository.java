package br.jus.tjpb.polvo_api.domain;

import java.util.Optional;

public interface SetorRepository extends DomainEntityRepository<Setor> {
    Optional<Setor> findByNome(String nome);
}
