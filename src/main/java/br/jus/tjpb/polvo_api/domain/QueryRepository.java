package br.jus.tjpb.polvo_api.domain;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
public interface QueryRepository<T extends DomainEntity> extends Repository<T, Long>, JpaSpecificationExecutor<T> {
}
