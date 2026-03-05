package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.config.command.AbstractCommand;
import br.jus.tjpb.polvo_api.config.security.AppUser;

public class CreateMetaCommand extends AbstractCommand {
    private final String titulo;
    private final String descricao;

    public CreateMetaCommand(AppUser user, String titulo, String descricao) {
        super(user);
        this.titulo = titulo;
        this.descricao = descricao;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }
}
