package br.jus.tjpb.polvo_api.config.command;

import br.jus.tjpb.polvo_api.config.security.AppUser;
import java.time.LocalDateTime;

public abstract class AbstractCommand {
    private final AppUser user;
    private final LocalDateTime timestamp;

    protected AbstractCommand(AppUser user) {
        this.user = user;
        this.timestamp = LocalDateTime.now();
    }

    public AppUser getUser() {
        return user;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
