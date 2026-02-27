package br.jus.tjpb.polvo_api.config;

import org.javers.spring.auditable.AuthorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
public class JaversConfig {

    @Bean
    public AuthorProvider provideJaversAuthor() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                String username = jwt.getClaimAsString("preferred_username");
                if (username != null && !username.isBlank()) {
                    return username;
                }
            }
            return "sistema";
        };
    }
}
