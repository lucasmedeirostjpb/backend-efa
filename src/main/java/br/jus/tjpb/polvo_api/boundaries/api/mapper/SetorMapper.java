package br.jus.tjpb.polvo_api.boundaries.api.mapper;

import br.jus.tjpb.polvo_api.boundaries.api.dto.SetorDTO;
import br.jus.tjpb.polvo_api.domain.Setor;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import br.jus.tjpb.polvo_api.boundaries.api.dto.SetorRequestDTO;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SetorMapper {
    SetorDTO toDTO(Setor entity);

    @Mapping(target = "id", ignore = true)
    Setor toEntity(SetorRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDTO(SetorRequestDTO dto, @MappingTarget Setor entity);
}
