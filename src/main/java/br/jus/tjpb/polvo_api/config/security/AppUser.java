package br.jus.tjpb.polvo_api.config.security;

import java.util.Set;

public record AppUser(
        String id,
        String name,
        Set<AppUserRoles> roles) {
}
