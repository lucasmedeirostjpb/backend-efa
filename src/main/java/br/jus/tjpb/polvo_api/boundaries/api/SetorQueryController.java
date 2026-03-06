package br.jus.tjpb.polvo_api.boundaries.api;

import br.jus.tjpb.polvo_api.boundaries.api.dto.SetorDTO;
import br.jus.tjpb.polvo_api.boundaries.api.dto.SetorRequestDTO;
import br.jus.tjpb.polvo_api.boundaries.api.mapper.SetorMapper;
import br.jus.tjpb.polvo_api.domain.Setor;
import br.jus.tjpb.polvo_api.domain.SetorRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/setores")
public class SetorQueryController {

    private final SetorRepository setorRepository;
    private final SetorMapper setorMapper;

    public SetorQueryController(SetorRepository setorRepository, SetorMapper setorMapper) {
        this.setorRepository = setorRepository;
        this.setorMapper = setorMapper;
    }

    @GetMapping
    public List<SetorDTO> listarTodos() {
        return setorRepository.findAll().stream()
                .map(setorMapper::toDTO)
                .collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COORDENADOR', 'DIGOV')")
    @Transactional
    public ResponseEntity<SetorDTO> criar(@Valid @RequestBody SetorRequestDTO dto) {
        Setor novoSetor = setorMapper.toEntity(dto);
        Setor salvo = Objects.requireNonNull(setorRepository.save(Objects.requireNonNull(novoSetor)));
        return ResponseEntity.status(HttpStatus.CREATED).body(setorMapper.toDTO(salvo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COORDENADOR', 'DIGOV')")
    @Transactional
    public ResponseEntity<SetorDTO> atualizar(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody SetorRequestDTO dto) {
        return setorRepository.findById(Objects.requireNonNull(id))
                .map(setor -> {
                    setorMapper.updateEntityFromDTO(dto, Objects.requireNonNull(setor));
                    Setor atualizado = Objects.requireNonNull(setorRepository.save(Objects.requireNonNull(setor)));
                    return ResponseEntity.ok(setorMapper.toDTO(atualizado));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
