package br.jus.tjpb.polvo_api.config.security;

import br.jus.tjpb.polvo_api.domain.Coordenador;
import br.jus.tjpb.polvo_api.domain.Meta;
import br.jus.tjpb.polvo_api.domain.MetaRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component("metaSecurity")
public class MetaSecurityValidator {

    private final MetaRepository metaRepository;

    public MetaSecurityValidator(MetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    @Transactional(readOnly = true)
    public boolean isDonoDaMeta(Long metaId, Jwt jwt) {
        if (metaId == null || jwt == null) {
            return false;
        }

        String loginKeycloak = jwt.getClaimAsString("preferred_username");
        if (loginKeycloak == null || loginKeycloak.isBlank()) {
            return false;
        }

        return metaRepository.findById(metaId)
                .map(Meta::getCoordenador)
                .map(Coordenador::getLoginKeycloak)
                .filter(login -> !login.isBlank())
                .map(login -> Objects.equals(login, loginKeycloak))
                .orElse(false);
    }
}