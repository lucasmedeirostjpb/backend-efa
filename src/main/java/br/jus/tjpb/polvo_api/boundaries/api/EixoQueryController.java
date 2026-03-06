package br.jus.tjpb.polvo_api.boundaries.api;

import br.jus.tjpb.polvo_api.boundaries.api.dto.EixoDTO;
import br.jus.tjpb.polvo_api.boundaries.api.dto.EixoRequestDTO;
import br.jus.tjpb.polvo_api.boundaries.api.mapper.EixoMapper;
import br.jus.tjpb.polvo_api.domain.EixoTematico;
import br.jus.tjpb.polvo_api.domain.EixoTematicoRepository;
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
@RequestMapping("/api/eixos")
public class EixoQueryController {

    private final EixoTematicoRepository eixoRepository;
    private final EixoMapper eixoMapper;

    public EixoQueryController(EixoTematicoRepository eixoRepository, EixoMapper eixoMapper) {
        this.eixoRepository = eixoRepository;
        this.eixoMapper = eixoMapper;
    }

    @GetMapping
    public List<EixoDTO> listarTodos() {
        return eixoRepository.findAll().stream()
                .map(eixoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COORDENADOR', 'DIGOV')")
    @Transactional
    public ResponseEntity<EixoDTO> criar(@Valid @RequestBody EixoRequestDTO dto) {
        EixoTematico novoEixo = eixoMapper.toEntity(dto);
        EixoTematico salvo = Objects.requireNonNull(eixoRepository.save(Objects.requireNonNull(novoEixo)));
        return ResponseEntity.status(HttpStatus.CREATED).body(eixoMapper.toDTO(salvo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COORDENADOR', 'DIGOV')")
    @Transactional
    public ResponseEntity<EixoDTO> atualizar(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody EixoRequestDTO dto) {
        return eixoRepository.findById(Objects.requireNonNull(id))
                .map(eixo -> {
                    eixoMapper.updateEntityFromDTO(dto, Objects.requireNonNull(eixo));
                    EixoTematico atualizado = Objects.requireNonNull(eixoRepository.save(Objects.requireNonNull(eixo)));
                    return ResponseEntity.ok(eixoMapper.toDTO(atualizado));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
