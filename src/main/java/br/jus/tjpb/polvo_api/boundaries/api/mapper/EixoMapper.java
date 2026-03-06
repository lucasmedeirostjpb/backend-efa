package br.jus.tjpb.polvo_api.boundaries.api.mapper;

import br.jus.tjpb.polvo_api.boundaries.api.dto.EixoDTO;
import br.jus.tjpb.polvo_api.domain.EixoTematico;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import br.jus.tjpb.polvo_api.boundaries.api.dto.EixoRequestDTO;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EixoMapper {
    EixoDTO toDTO(EixoTematico entity);

    @Mapping(target = "id", ignore = true)
    EixoTematico toEntity(EixoRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDTO(EixoRequestDTO dto, @MappingTarget EixoTematico entity);
}
