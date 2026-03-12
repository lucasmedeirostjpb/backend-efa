package br.jus.tjpb.polvo_api.boundaries.api.mapper;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaResponseDTO;
import br.jus.tjpb.polvo_api.domain.Coordenador;
import br.jus.tjpb.polvo_api.domain.Delegacao;
import br.jus.tjpb.polvo_api.domain.Meta;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetaMapperTest {

    private final MetaMapper metaMapper = Mappers.getMapper(MetaMapper.class);

    @Test
    void shouldExposeDelegadosEmailsInMetaResponseDTO() {
        Coordenador coordenador = new Coordenador("Coordenador Teste");
        coordenador.setLoginKeycloak("12345678900");
        coordenador.setDelegacoes(List.of(
                new Delegacao(coordenador, "delegado1@tjpb.jus.br", "Delegado Um"),
                new Delegacao(coordenador, "delegado2@tjpb.jus.br", "Delegado Dois")));

        Meta meta = new Meta();
        meta.setCoordenador(coordenador);

        MetaResponseDTO dto = metaMapper.toDTO(meta);

        assertEquals(List.of("delegado1@tjpb.jus.br", "delegado2@tjpb.jus.br"), dto.getDelegadosEmails());
    }
}