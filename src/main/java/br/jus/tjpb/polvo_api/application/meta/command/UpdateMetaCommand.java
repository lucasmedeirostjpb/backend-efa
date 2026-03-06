package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaRequestDTO;
import br.jus.tjpb.polvo_api.config.command.AbstractCommand;
import br.jus.tjpb.polvo_api.config.security.AppUser;

public class UpdateMetaCommand extends AbstractCommand {
    private final Long id;
    private final MetaRequestDTO dto;

    public UpdateMetaCommand(AppUser user, Long id, MetaRequestDTO dto) {
        super(user);
        this.id = id;
        this.dto = dto;
    }

    public Long getId() {
        return id;
    }

    public MetaRequestDTO getDto() {
        return dto;
    }
}
