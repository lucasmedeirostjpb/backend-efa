package br.jus.tjpb.polvo_api.boundaries.api;

import br.jus.tjpb.polvo_api.application.meta.command.*;
import br.jus.tjpb.polvo_api.config.security.AppUserResolver;
import br.jus.tjpb.polvo_api.domain.Meta;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/metas")
public class MetaCommandController {

    private final CreateMetaCommandHandler createHandler;
    private final UpdateMetaCommandHandler updateHandler;
    private final DeleteMetaCommandHandler deleteHandler;
    private final AppUserResolver appUserResolver;

    public MetaCommandController(CreateMetaCommandHandler createHandler,
            UpdateMetaCommandHandler updateHandler,
            DeleteMetaCommandHandler deleteHandler,
            AppUserResolver appUserResolver) {
        this.createHandler = createHandler;
        this.updateHandler = updateHandler;
        this.deleteHandler = deleteHandler;
        this.appUserResolver = appUserResolver;
    }

    @PostMapping
    public ResponseEntity<Meta> criar(@RequestBody Meta meta) {
        var command = new CreateMetaCommand(appUserResolver.resolveCurrentUser(), meta.getTitulo(),
                meta.getDescricao());
        return ResponseEntity.ok(createHandler.handle(command));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Meta> atualizar(@PathVariable Long id, @RequestBody Meta metaAtualizada) {
        var command = new UpdateMetaCommand(
                appUserResolver.resolveCurrentUser(),
                id,
                metaAtualizada.getTitulo(),
                metaAtualizada.getDescricao(),
                metaAtualizada.getConcluida());
        return ResponseEntity.ok(updateHandler.handle(command));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        var command = new DeleteMetaCommand(appUserResolver.resolveCurrentUser(), id);
        deleteHandler.handle(command);
        return ResponseEntity.noContent().build();
    }
}
