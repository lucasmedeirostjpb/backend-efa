package br.jus.tjpb.polvo_api.config.security;

import br.jus.tjpb.polvo_api.domain.Coordenador;
import br.jus.tjpb.polvo_api.domain.Delegacao;
import br.jus.tjpb.polvo_api.domain.Meta;
import br.jus.tjpb.polvo_api.domain.MetaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaSecurityValidatorTest {

    @Mock
    private MetaRepository metaRepository;

    @InjectMocks
    private MetaSecurityValidator metaSecurityValidator;

    @Test
    void shouldReturnTrueWhenJwtUserOwnsMeta() {
        Meta meta = new Meta();
        Coordenador coordenador = new Coordenador("Coordenador Teste");
        coordenador.setLoginKeycloak("coord.teste");
        meta.setCoordenador(coordenador);

        when(metaRepository.findById(10L)).thenReturn(Optional.of(meta));

        assertTrue(metaSecurityValidator.isDonoDaMeta(10L, buildJwt("coord.teste", null)));
    }

    @Test
    void shouldReturnFalseWhenJwtUserDoesNotOwnMeta() {
        Meta meta = new Meta();
        Coordenador coordenador = new Coordenador("Coordenador Teste");
        coordenador.setLoginKeycloak("coord.dono");
        coordenador.setDelegacoes(List.of());
        meta.setCoordenador(coordenador);

        when(metaRepository.findById(10L)).thenReturn(Optional.of(meta));

        assertFalse(metaSecurityValidator.isDonoDaMeta(10L, buildJwt("coord.outro", null)));
    }

    @Test
    void shouldReturnFalseWhenMetaDoesNotExist() {
        when(metaRepository.findById(10L)).thenReturn(Optional.empty());

        assertFalse(metaSecurityValidator.isDonoDaMeta(10L, buildJwt("coord.teste", null)));
    }

    @Test
    void shouldReturnTrueWhenJwtUserIsDelegadoDoCoordenador() {
        Meta meta = new Meta();
        Coordenador coordenador = new Coordenador("Coordenador Teste");
        coordenador.setLoginKeycloak("coord.dono");
        Delegacao delegacao = new Delegacao(coordenador, "delegado@tjpb.jus.br", "Delegado Teste");
        coordenador.setDelegacoes(List.of(delegacao));
        meta.setCoordenador(coordenador);

        when(metaRepository.findById(10L)).thenReturn(Optional.of(meta));

        assertTrue(metaSecurityValidator.isDonoDaMeta(10L, buildJwt("coord.outro", "delegado@tjpb.jus.br")));
    }

    private Jwt buildJwt(String preferredUsername, String email) {
        Map<String, Object> claims = new java.util.HashMap<>();
        if (preferredUsername != null) {
            claims.put("preferred_username", preferredUsername);
        }
        if (email != null) {
            claims.put("email", email);
        }

        return new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                claims);
    }
}