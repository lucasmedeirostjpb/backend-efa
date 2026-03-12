package br.jus.tjpb.polvo_api.boundaries.api;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaAcompanhamentoRequestDTO;
import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MetaCommandControllerAuthorizationTest {

    @Test
    void shouldRestrictStructuralUpdateToDigov() throws NoSuchMethodException {
        Method method = MetaCommandController.class.getMethod(
                "atualizar",
                Long.class,
                MetaRequestDTO.class,
                Jwt.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        PutMapping putMapping = method.getAnnotation(PutMapping.class);

        assertNotNull(preAuthorize);
        assertNotNull(putMapping);
        assertEquals("hasRole('DIGOV')", preAuthorize.value());
        assertArrayEquals(new String[] { "/{id}" }, putMapping.value());
    }

    @Test
    void shouldExposeDedicatedAcompanhamentoEndpointForOwnerCoordenador() throws NoSuchMethodException {
        Method method = MetaCommandController.class.getMethod(
                "atualizarAcompanhamento",
                Long.class,
                MetaAcompanhamentoRequestDTO.class,
                Jwt.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        PutMapping putMapping = method.getAnnotation(PutMapping.class);

        assertNotNull(preAuthorize);
        assertNotNull(putMapping);
        assertEquals(
            "hasRole('DIGOV') or @metaSecurity.isDonoDaMeta(#id, #jwt)",
                preAuthorize.value());
        assertArrayEquals(new String[] { "/{id}/acompanhamento" }, putMapping.value());
    }
}