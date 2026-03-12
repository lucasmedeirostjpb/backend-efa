package br.jus.tjpb.polvo_api.boundaries.api;

import br.jus.tjpb.polvo_api.boundaries.api.dto.DelegacaoRequestDTO;
import br.jus.tjpb.polvo_api.boundaries.api.dto.DelegacaoResponseDTO;
import br.jus.tjpb.polvo_api.config.security.AppUserResolver;
import br.jus.tjpb.polvo_api.config.security.AppUserRoles;
import br.jus.tjpb.polvo_api.domain.Coordenador;
import br.jus.tjpb.polvo_api.domain.CoordenadorRepository;
import br.jus.tjpb.polvo_api.domain.Delegacao;
import br.jus.tjpb.polvo_api.domain.DelegacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DelegacaoControllerTest {

    @Mock
    private DelegacaoRepository delegacaoRepository;
    @Mock
    private CoordenadorRepository coordenadorRepository;

    private DelegacaoController controller;

    @Test
    void shouldListDelegacoesForLoggedCoordenador() {
        controller = buildController();
        Coordenador coordenador = buildCoordenador(1L, "12345678900");
        Delegacao delegacao = new Delegacao(coordenador, "delegado1@tjpb.jus.br", "Delegado Um");

        authenticateAs("12345678900", AppUserRoles.COORDENADOR);
        when(coordenadorRepository.findByLoginKeycloak("12345678900")).thenReturn(Optional.of(coordenador));
        when(delegacaoRepository.findAllByCoordenadorIdOrderByDelegadoNomeAsc(1L)).thenReturn(List.of(delegacao));

        List<DelegacaoResponseDTO> response = controller.listarMinhasDelegacoes();

        assertEquals(1, response.size());
        assertEquals("delegado1@tjpb.jus.br", response.getFirst().getDelegadoEmail());
        assertEquals("Delegado Um", response.getFirst().getDelegadoNome());
    }

    @Test
    @SuppressWarnings("null")
    void shouldCreateDelegacaoForLoggedCoordenador() {
        controller = buildController();
        Coordenador coordenador = buildCoordenador(1L, "12345678900");
        DelegacaoRequestDTO request = new DelegacaoRequestDTO();
        request.setDelegadoEmail("delegado1@tjpb.jus.br");
        request.setDelegadoNome("Delegado Um");

        authenticateAs("12345678900", AppUserRoles.COORDENADOR);
        when(coordenadorRepository.findByLoginKeycloak("12345678900")).thenReturn(Optional.of(coordenador));
        when(delegacaoRepository.existsByCoordenadorIdAndDelegadoEmail(1L, "delegado1@tjpb.jus.br")).thenReturn(false);
        when(delegacaoRepository.save(any(Delegacao.class))).thenAnswer(invocation -> invocation.getArgument(0, Delegacao.class));

        var response = controller.criarDelegacao(request);

        verify(delegacaoRepository).save(argThat(savedDelegacao -> {
            assertEquals(coordenador, savedDelegacao.getCoordenador());
            assertEquals("delegado1@tjpb.jus.br", savedDelegacao.getDelegadoEmail());
            assertEquals("Delegado Um", savedDelegacao.getDelegadoNome());
            return true;
        }));
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void shouldRejectDuplicatedDelegacao() {
        controller = buildController();
        Coordenador coordenador = buildCoordenador(1L, "12345678900");
        DelegacaoRequestDTO request = new DelegacaoRequestDTO();
        request.setDelegadoEmail("delegado1@tjpb.jus.br");
        request.setDelegadoNome("Delegado Um");

        authenticateAs("12345678900", AppUserRoles.COORDENADOR);
        when(coordenadorRepository.findByLoginKeycloak("12345678900")).thenReturn(Optional.of(coordenador));
        when(delegacaoRepository.existsByCoordenadorIdAndDelegadoEmail(1L, "delegado1@tjpb.jus.br")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.criarDelegacao(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void shouldDeleteDelegacaoOwnedByLoggedCoordenador() {
        controller = buildController();
        Coordenador coordenador = buildCoordenador(1L, "12345678900");
        Delegacao delegacao = new Delegacao(coordenador, "delegado1@tjpb.jus.br", "Delegado Um");

        authenticateAs("12345678900", AppUserRoles.COORDENADOR);
        when(coordenadorRepository.findByLoginKeycloak("12345678900")).thenReturn(Optional.of(coordenador));
        when(delegacaoRepository.findByIdAndCoordenadorId(55L, 1L)).thenReturn(Optional.of(delegacao));

        var response = controller.excluirDelegacao(55L);

        verify(delegacaoRepository).delete(delegacao);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void shouldFailWhenLoggedUserIsNotMappedToCoordenador() {
        controller = buildController();
        authenticateAs("12345678900");
        when(coordenadorRepository.findByLoginKeycloak("12345678900")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                controller::listarMinhasDelegacoes);

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(Objects.requireNonNull(exception.getReason()).contains("Coordenador logado não encontrado"));
    }

    private Coordenador buildCoordenador(Long id, String loginKeycloak) {
        Coordenador coordenador = new Coordenador("Coordenador Teste");
        coordenador.setId(id);
        coordenador.setLoginKeycloak(loginKeycloak);
        return coordenador;
    }

    private DelegacaoController buildController() {
        SecurityContextHolder.clearContext();
        return new DelegacaoController(delegacaoRepository, coordenadorRepository, new AppUserResolver());
    }

    private void authenticateAs(String login, AppUserRoles... roles) {
        var authorities = Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", authorities));
    }
}