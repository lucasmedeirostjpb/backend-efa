package br.jus.tjpb.polvo_api.domain;

import org.springframework.data.jpa.domain.Specification;

@FunctionalInterface
public interface SpecificationBuilder<T, F> {
    Specification<T> build(F filter);
}
