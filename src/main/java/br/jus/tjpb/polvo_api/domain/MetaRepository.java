package br.jus.tjpb.polvo_api.domain;

import br.jus.tjpb.polvo_api.boundaries.api.dto.DashboardKpiDTO;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.jpa.repository.Query;

@JaversSpringDataAuditable
public interface MetaRepository extends DomainEntityRepository<Meta>, QueryRepository<Meta> {

    @Query("SELECT new br.jus.tjpb.polvo_api.boundaries.api.dto.DashboardKpiDTO(" +
            "COUNT(m.id), " +
            "SUM(CASE WHEN m.status <> br.jus.tjpb.polvo_api.domain.StatusMeta.NAO_SE_APLICA THEN m.pMaximo ELSE 0 END), "
            +
            "SUM(m.pontosAtingidos)) " +
            "FROM Meta m")
    DashboardKpiDTO obterKpisGlobaisRaw();
}
