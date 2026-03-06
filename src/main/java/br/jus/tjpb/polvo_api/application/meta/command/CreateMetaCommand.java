package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaRequestDTO;
import br.jus.tjpb.polvo_api.config.command.AbstractCommand;
import br.jus.tjpb.polvo_api.config.security.AppUser;

public class CreateMetaCommand extends AbstractCommand {
    private final MetaRequestDTO dto;

    public CreateMetaCommand(AppUser user, MetaRequestDTO dto) {
        super(user);
        this.dto = dto;
    }

    public MetaRequestDTO getDto() {
        return dto;
    }
}
