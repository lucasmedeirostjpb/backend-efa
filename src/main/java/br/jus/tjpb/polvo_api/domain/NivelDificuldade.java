package br.jus.tjpb.polvo_api.domain;

import lombok.Getter;

@Getter
public enum NivelDificuldade {
    SEM_DIFICULDADES("Sem dificuldades"),
    EM_ALERTA("Em alerta"),
    SITUACAO_CRITICA("Situação crítica");

    private final String descricao;

    NivelDificuldade(String descricao) {
        this.descricao = descricao;
    }
}
