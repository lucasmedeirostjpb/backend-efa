package br.jus.tjpb.polvo_api.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AppUserResolver {

    public AppUser resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String id = authentication.getName();
        Set<AppUserRoles> roles = authentication.getAuthorities().stream()
                .map(grantedAuthority -> {
                    try {
                        String roleStr = grantedAuthority.getAuthority().replace("ROLE_", "");
                        return AppUserRoles.valueOf(roleStr);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        return new AppUser(id, id, roles);
    }
}
