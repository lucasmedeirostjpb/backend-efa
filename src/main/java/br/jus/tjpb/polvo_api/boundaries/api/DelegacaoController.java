package br.jus.tjpb.polvo_api.boundaries.api;

import br.jus.tjpb.polvo_api.boundaries.api.dto.DelegacaoRequestDTO;
import br.jus.tjpb.polvo_api.boundaries.api.dto.DelegacaoResponseDTO;
import br.jus.tjpb.polvo_api.config.security.AppUser;
import br.jus.tjpb.polvo_api.config.security.AppUserResolver;
import br.jus.tjpb.polvo_api.domain.Coordenador;
import br.jus.tjpb.polvo_api.domain.CoordenadorRepository;
import br.jus.tjpb.polvo_api.domain.Delegacao;
import br.jus.tjpb.polvo_api.domain.DelegacaoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/coordenadores/me/delegacoes")
public class DelegacaoController {

    private final DelegacaoRepository delegacaoRepository;
    private final CoordenadorRepository coordenadorRepository;
    private final AppUserResolver appUserResolver;

    public DelegacaoController(DelegacaoRepository delegacaoRepository,
            CoordenadorRepository coordenadorRepository,
            AppUserResolver appUserResolver) {
        this.delegacaoRepository = delegacaoRepository;
        this.coordenadorRepository = coordenadorRepository;
        this.appUserResolver = appUserResolver;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<DelegacaoResponseDTO> listarMinhasDelegacoes() {
        Coordenador coordenador = resolveCurrentCoordenador();

        return delegacaoRepository.findAllByCoordenadorIdOrderByDelegadoNomeAsc(coordenador.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<DelegacaoResponseDTO> criarDelegacao(@Valid @RequestBody DelegacaoRequestDTO dto) {
        Coordenador coordenador = resolveCurrentCoordenador();
        String delegadoEmail = dto.getDelegadoEmail().trim();

        if (delegacaoRepository.existsByCoordenadorIdAndDelegadoEmail(coordenador.getId(), delegadoEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delegação já cadastrada para este e-mail.");
        }

        Delegacao delegacao = new Delegacao(coordenador, delegadoEmail, dto.getDelegadoNome().trim());
        Delegacao savedDelegacao = delegacaoRepository.save(delegacao);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(savedDelegacao));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<Void> excluirDelegacao(@PathVariable Long id) {
        Coordenador coordenador = resolveCurrentCoordenador();

        Delegacao delegacao = delegacaoRepository.findByIdAndCoordenadorId(id, coordenador.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delegação não encontrada."));

        delegacaoRepository.delete(delegacao);
        return ResponseEntity.noContent().build();
    }

    private Coordenador resolveCurrentCoordenador() {
        AppUser user = appUserResolver.resolveCurrentUser();
        if (user == null || user.id() == null || user.id().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário autenticado não encontrado.");
        }

        return coordenadorRepository.findByLoginKeycloak(user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coordenador logado não encontrado."));
    }

    private DelegacaoResponseDTO toResponseDTO(Delegacao delegacao) {
        DelegacaoResponseDTO dto = new DelegacaoResponseDTO();
        dto.setId(delegacao.getId());
        dto.setDelegadoEmail(delegacao.getDelegadoEmail());
        dto.setDelegadoNome(delegacao.getDelegadoNome());
        return dto;
    }
}