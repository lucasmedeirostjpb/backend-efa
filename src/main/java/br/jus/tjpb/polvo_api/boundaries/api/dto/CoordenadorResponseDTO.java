package br.jus.tjpb.polvo_api.boundaries.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CoordenadorResponseDTO {
    private Long id;
    private String nome;
    private String email;
    private String loginKeycloak;
}
