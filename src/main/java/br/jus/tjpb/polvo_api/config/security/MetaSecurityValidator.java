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
        String delegateEmail = jwt.getClaimAsString("email");

        return metaRepository.findById(metaId)
                .map(Meta::getCoordenador)
                .map(coordenador -> isOwnerOrDelegate(coordenador, loginKeycloak, delegateEmail))
                .orElse(false);
    }

    private boolean isOwnerOrDelegate(Coordenador coordenador, String loginKeycloak, String delegateEmail) {
        String ownerLogin = coordenador.getLoginKeycloak();
        if (ownerLogin != null && !ownerLogin.isBlank() && Objects.equals(ownerLogin, loginKeycloak)) {
            return true;
        }

        if (delegateEmail == null || delegateEmail.isBlank() || coordenador.getDelegacoes() == null) {
            return false;
        }

        return coordenador.getDelegacoes().stream()
                .map(br.jus.tjpb.polvo_api.domain.Delegacao::getDelegadoEmail)
                .filter(email -> email != null && !email.isBlank())
                .anyMatch(email -> Objects.equals(email, delegateEmail));
    }
}