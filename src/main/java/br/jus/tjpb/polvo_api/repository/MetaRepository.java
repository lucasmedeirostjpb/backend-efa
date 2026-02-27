package br.jus.tjpb.polvo_api.repository;

import br.jus.tjpb.polvo_api.model.Meta;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.jpa.repository.JpaRepository;

@JaversSpringDataAuditable
public interface MetaRepository extends JpaRepository<Meta, Long> {
}
