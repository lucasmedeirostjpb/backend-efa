package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.domain.Meta;
import br.jus.tjpb.polvo_api.domain.MetaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateMetaCommandHandler {
    private final MetaRepository metaRepository;

    public UpdateMetaCommandHandler(MetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    @Transactional
    public Meta handle(UpdateMetaCommand command) {
        Long id = java.util.Objects.requireNonNull(command.getId(), "ID não pode ser nulo");
        Meta meta = metaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meta não encontrada"));

        meta.setTitulo(command.getTitulo());
        meta.setDescricao(command.getDescricao());
        if (command.getConcluida() != null) {
            meta.setConcluida(command.getConcluida());
        }
        return metaRepository.save(meta);
    }
}
