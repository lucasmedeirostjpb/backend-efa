package br.jus.tjpb.polvo_api.boundaries.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EixoRequestDTO {
    @NotBlank(message = "O nome é obrigatório.")
    private String nome;
}
