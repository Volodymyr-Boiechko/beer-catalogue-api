package com.haufe.beercatalogue.security;

import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class OwnershipChecker {

    public boolean canEditManufacturer(Long manufacturerId) {
        return getPrincipal()
            .map(details -> details.isAdmin()
                || (manufacturerId != null && manufacturerId.equals(details.getManufacturerId())))
            .orElse(false);
    }

    public boolean isAdmin() {
        return getPrincipal()
            .map(AppUserDetails::isAdmin)
            .orElse(false);
    }

    private Optional<AppUserDetails> getPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails details) {
            return Optional.of(details);
        }
        return Optional.empty();
    }
}
