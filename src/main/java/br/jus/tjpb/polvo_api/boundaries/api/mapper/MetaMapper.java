package br.jus.tjpb.polvo_api.boundaries.api.mapper;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaRequestDTO;
import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaResponseDTO;
import br.jus.tjpb.polvo_api.domain.Meta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = {
        br.jus.tjpb.polvo_api.domain.EixoTematico.class,
        br.jus.tjpb.polvo_api.domain.Setor.class,
        br.jus.tjpb.polvo_api.domain.NivelDificuldade.class
})
public interface MetaMapper {

    @Mapping(target = "eixo", ignore = true)
    @Mapping(target = "setor", ignore = true)
    @Mapping(target = "coordenador", ignore = true)
    Meta toEntity(MetaRequestDTO dto);

    @Mapping(target = "eixoId", source = "eixo.id")
    @Mapping(target = "eixoNome", source = "eixo.nome")
    @Mapping(target = "setorId", source = "setor.id")
    @Mapping(target = "setorSigla", source = "setor.sigla")
    @Mapping(target = "setorNome", source = "setor.nome")
    @Mapping(target = "coordenadorId", source = "coordenador.id")
    @Mapping(target = "coordenadorNome", source = "coordenador.nome")
    MetaResponseDTO toDTO(Meta entity);

    @Mapping(target = "eixo", ignore = true)
    @Mapping(target = "setor", ignore = true)
    @Mapping(target = "coordenador", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDTO(MetaRequestDTO dto, @org.mapstruct.MappingTarget Meta entity);
}
