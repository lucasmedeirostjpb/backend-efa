package br.jus.tjpb.polvo_api.boundaries.api.mapper;

import br.jus.tjpb.polvo_api.boundaries.api.dto.CoordenadorResponseDTO;
import br.jus.tjpb.polvo_api.domain.Coordenador;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CoordenadorMapper {
    CoordenadorResponseDTO toDTO(Coordenador entity);
}
