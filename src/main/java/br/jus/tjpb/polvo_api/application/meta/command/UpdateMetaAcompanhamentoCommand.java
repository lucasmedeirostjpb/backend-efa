package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaAcompanhamentoRequestDTO;
import br.jus.tjpb.polvo_api.config.command.AbstractCommand;
import br.jus.tjpb.polvo_api.config.security.AppUser;

public class UpdateMetaAcompanhamentoCommand extends AbstractCommand {
    private final Long id;
    private final MetaAcompanhamentoRequestDTO dto;

    public UpdateMetaAcompanhamentoCommand(AppUser user, Long id, MetaAcompanhamentoRequestDTO dto) {
        super(user);
        this.id = id;
        this.dto = dto;
    }

    public Long getId() {
        return id;
    }

    public MetaAcompanhamentoRequestDTO getDto() {
        return dto;
    }
}