package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.domain.MetaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteMetaCommandHandler {
    private final MetaRepository metaRepository;

    public DeleteMetaCommandHandler(MetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    @Transactional
    public void handle(DeleteMetaCommand command) {
        Long id = java.util.Objects.requireNonNull(command.getId(), "ID não pode ser nulo");
        if (!metaRepository.existsById(id)) {
            throw new IllegalArgumentException("Meta não encontrada");
        }
        metaRepository.deleteById(id);
    }
}
