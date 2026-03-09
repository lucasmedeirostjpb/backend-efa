package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.boundaries.api.dto.MetaRequestDTO;
import br.jus.tjpb.polvo_api.config.command.AbstractCommand;
import br.jus.tjpb.polvo_api.config.security.AppUser;

import java.util.List;

public class CreateMetaBatchCommand extends AbstractCommand {
    private final List<MetaRequestDTO> dtos;

    public CreateMetaBatchCommand(AppUser user, List<MetaRequestDTO> dtos) {
        super(user);
        this.dtos = dtos;
    }

    public List<MetaRequestDTO> getDtos() {
        return dtos;
    }
}
