package br.jus.tjpb.polvo_api.controller;

import br.jus.tjpb.polvo_api.model.Meta;
import br.jus.tjpb.polvo_api.repository.MetaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/metas")
public class MetaController {

    private final MetaRepository metaRepository;

    public MetaController(MetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    @GetMapping
    public List<Meta> listarTodas() {
        return metaRepository.findAll();
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
}
