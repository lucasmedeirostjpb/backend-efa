package br.jus.tjpb.polvo_api.controller;

import br.jus.tjpb.polvo_api.model.Meta;
import br.jus.tjpb.polvo_api.repository.MetaRepository;
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

@RestController
@RequestMapping("/api/metas")
public class MetaController {

    private final MetaRepository metaRepository;

    public MetaController(MetaRepository metaRepository) {
        this.metaRepository = metaRepository;
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
}
