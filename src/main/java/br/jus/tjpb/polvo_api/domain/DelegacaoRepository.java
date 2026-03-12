package br.jus.tjpb.polvo_api.domain;

import java.util.List;
import java.util.Optional;

public interface DelegacaoRepository extends DomainEntityRepository<Delegacao> {
    boolean existsByCoordenadorIdAndDelegadoEmail(Long coordenadorId, String delegadoEmail);

    List<Delegacao> findAllByCoordenadorIdOrderByDelegadoNomeAsc(Long coordenadorId);

    Optional<Delegacao> findByIdAndCoordenadorId(Long id, Long coordenadorId);
}