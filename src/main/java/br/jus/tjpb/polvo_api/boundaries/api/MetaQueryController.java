package br.jus.tjpb.polvo_api.boundaries.api;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaResponseDTO;
import br.jus.tjpb.polvo_api.boundaries.api.mapper.MetaMapper;
import br.jus.tjpb.polvo_api.domain.Meta;
import br.jus.tjpb.polvo_api.domain.MetaRepository;
import br.jus.tjpb.polvo_api.shared.dto.HistoricoAlteracaoDTO;
import br.jus.tjpb.polvo_api.shared.dto.PropriedadeAlteradaDTO;
import org.javers.core.Javers;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metas")
@Transactional(readOnly = true)
public class MetaQueryController {

    private final MetaRepository metaRepository;
    private final MetaMapper metaMapper;
    private final Javers javers;

    public MetaQueryController(MetaRepository metaRepository, MetaMapper metaMapper, Javers javers) {
        this.metaRepository = metaRepository;
        this.metaMapper = metaMapper;
        this.javers = javers;
    }

    @GetMapping
    public Page<MetaResponseDTO> listarTodas(
            @PageableDefault(size = 20, sort = "titulo", direction = Sort.Direction.ASC) @org.springframework.lang.NonNull Pageable pageable) {
        return metaRepository.findAll(pageable).map(metaMapper::toDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MetaResponseDTO> buscarPorId(@PathVariable @org.springframework.lang.NonNull Long id) {
        return metaRepository.findById(id)
                .map(metaMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<List<HistoricoAlteracaoDTO>> buscarHistorico(
            @PathVariable @org.springframework.lang.NonNull Long id) {
        if (!metaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        List<Change> changes = javers.findChanges(QueryBuilder.byInstanceId(id, Meta.class).build());

        Map<CommitMetadata, List<Change>> changesByCommit = changes.stream()
                .filter(change -> change.getCommitMetadata().isPresent())
                .collect(Collectors.groupingBy(change -> change.getCommitMetadata().get()));

        List<HistoricoAlteracaoDTO> historico = changesByCommit.entrySet().stream()
                .sorted((e1, e2) -> e2.getKey().getCommitDate().compareTo(e1.getKey().getCommitDate()))
                .map(entry -> {
                    CommitMetadata commit = entry.getKey();
                    List<Change> commitChanges = entry.getValue();

                    HistoricoAlteracaoDTO dto = new HistoricoAlteracaoDTO();
                    dto.setAutor(commit.getAuthor());
                    dto.setDataHora(commit.getCommitDate());

                    List<PropriedadeAlteradaDTO> props = new ArrayList<>();
                    String tipoMudanca = "ATUALIZACAO";

                    for (Change c : commitChanges) {
                        if (c instanceof NewObject) {
                            tipoMudanca = "CRIACAO";
                        } else if (c instanceof ObjectRemoved) {
                            tipoMudanca = "EXCLUSAO";
                        } else if (c instanceof ValueChange vc) {
                            PropriedadeAlteradaDTO propDto = new PropriedadeAlteradaDTO();
                            propDto.setPropriedade(vc.getPropertyName());
                            propDto.setValorAntigo(vc.getLeft());
                            propDto.setValorNovo(vc.getRight());
                            props.add(propDto);
                        }
                    }
                    dto.setTipoMudanca(tipoMudanca);
                    dto.setPropriedadesAlteradas(props);
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(historico);
    }
}
