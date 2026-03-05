package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.domain.Meta;
import br.jus.tjpb.polvo_api.domain.MetaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateMetaCommandHandler {
    private final MetaRepository metaRepository;

    public CreateMetaCommandHandler(MetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    @Transactional
    public Meta handle(CreateMetaCommand command) {
        Meta meta = new Meta();
        meta.setTitulo(command.getTitulo());
        meta.setDescricao(command.getDescricao());
        meta.setConcluida(false);
        return metaRepository.save(meta);
    }
}
