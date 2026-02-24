package br.jus.tjpb.polvo_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TesteController {

    @GetMapping("/api/public/teste")
    public ResponseEntity<String> testePublico() {
        return ResponseEntity.ok("Acesso livre - Portal de Transparência OK");
    }

    @GetMapping("/api/gestao/teste")
    public ResponseEntity<String> testeGestao() {
        return ResponseEntity.ok("Acesso autorizado - Ambiente de Gestão (Role COORDENADOR) OK");
    }
}
