package br.jus.tjpb.polvo_api.boundaries.api;

import br.jus.tjpb.polvo_api.application.meta.command.*;
import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaRequestDTO;
import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaResponseDTO;
import br.jus.tjpb.polvo_api.config.security.AppUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/metas")
@PreAuthorize("hasRole('COORDENADOR')")
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
    public ResponseEntity<MetaResponseDTO> criar(@Valid @RequestBody MetaRequestDTO dto) {
        var command = new CreateMetaCommand(appUserResolver.resolveCurrentUser(), dto);
        return ResponseEntity.ok(createHandler.handle(command));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MetaResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody MetaRequestDTO dto) {
        var command = new UpdateMetaCommand(
                appUserResolver.resolveCurrentUser(),
                id,
                dto);
        return ResponseEntity.ok(updateHandler.handle(command));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        var command = new DeleteMetaCommand(appUserResolver.resolveCurrentUser(), id);
        deleteHandler.handle(command);
        return ResponseEntity.noContent().build();
    }
}
