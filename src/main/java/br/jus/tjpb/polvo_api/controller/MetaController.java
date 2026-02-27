package br.jus.tjpb.polvo_api.controller;

import br.jus.tjpb.polvo_api.model.Meta;
import br.jus.tjpb.polvo_api.repository.MetaRepository;
import br.jus.tjpb.polvo_api.dto.HistoricoAlteracaoDTO;
import br.jus.tjpb.polvo_api.dto.PropriedadeAlteradaDTO;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metas")
public class MetaController {

    private final MetaRepository metaRepository;
    private final Javers javers;

    public MetaController(MetaRepository metaRepository, Javers javers) {
        this.metaRepository = metaRepository;
        this.javers = javers;
    }

    @GetMapping
    public Page<Meta> listarTodas(
            @PageableDefault(size = 20, sort = "dataCriacao", direction = Sort.Direction.DESC) Pageable pageable) {
        return metaRepository.findAll(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Meta> buscarPorId(@PathVariable Long id) {
        return metaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Meta criar(@RequestBody Meta meta) {
        return metaRepository.save(meta);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Meta> atualizar(@PathVariable Long id, @RequestBody Meta metaAtualizada) {
        return metaRepository.findById(id)
                .map(meta -> {
                    meta.setTitulo(metaAtualizada.getTitulo());
                    meta.setDescricao(metaAtualizada.getDescricao());
                    meta.setConcluida(metaAtualizada.getConcluida());
                    return ResponseEntity.ok(metaRepository.save(meta));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!metaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        metaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<List<HistoricoAlteracaoDTO>> buscarHistorico(@PathVariable Long id) {
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
                        } else if (c instanceof ValueChange) {
                            ValueChange vc = (ValueChange) c;
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
