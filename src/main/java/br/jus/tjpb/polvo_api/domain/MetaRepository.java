package br.jus.tjpb.polvo_api.domain;

import org.javers.spring.annotation.JaversSpringDataAuditable;

@JaversSpringDataAuditable
public interface MetaRepository extends DomainEntityRepository<Meta>, QueryRepository<Meta> {
}
