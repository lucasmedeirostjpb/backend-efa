package br.jus.tjpb.polvo_api.config;

import br.jus.tjpb.polvo_api.config.security.AppUser;
import br.jus.tjpb.polvo_api.config.security.AppUserResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    private final AppUserResolver appUserResolver;

    public JpaConfig(AppUserResolver appUserResolver) {
        this.appUserResolver = appUserResolver;
    }

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(appUserResolver.resolveCurrentUser())
                .map(AppUser::id)
                .or(() -> Optional.of("system"));
    }
}
