package br.jus.tjpb.polvo_api.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface DomainEntityRepository<T extends DomainEntity> extends JpaRepository<T, Long> {
}
