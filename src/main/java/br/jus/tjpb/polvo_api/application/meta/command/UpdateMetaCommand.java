package br.jus.tjpb.polvo_api.application.meta.command;

import br.jus.tjpb.polvo_api.config.command.AbstractCommand;
import br.jus.tjpb.polvo_api.config.security.AppUser;

public class UpdateMetaCommand extends AbstractCommand {
    private final Long id;
    private final String titulo;
    private final String descricao;
    private final Boolean concluida;

    public UpdateMetaCommand(AppUser user, Long id, String titulo, String descricao, Boolean concluida) {
        super(user);
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.concluida = concluida;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public Boolean getConcluida() {
        return concluida;
    }
}
