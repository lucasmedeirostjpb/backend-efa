package br.jus.tjpb.polvo_api.boundaries.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DelegacaoRequestDTO {

    @NotBlank(message = "O e-mail do delegado é obrigatório.")
    @Email(message = "O e-mail do delegado deve ser valido.")
    private String delegadoEmail;

    @NotBlank(message = "O nome do delegado é obrigatório.")
    private String delegadoNome;
}