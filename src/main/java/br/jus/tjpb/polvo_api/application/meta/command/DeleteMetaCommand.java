package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.config.command.AbstractCommand;
import br.jus.tjpb.polvo_api.config.security.AppUser;

public class DeleteMetaCommand extends AbstractCommand {
    private final Long id;

    public DeleteMetaCommand(AppUser user, Long id) {
        super(user);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
