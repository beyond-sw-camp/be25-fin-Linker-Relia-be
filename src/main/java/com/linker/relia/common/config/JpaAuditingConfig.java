package com.linker.relia.common.config;

import com.linker.relia.common.audit.AuditContextHolder;
import com.linker.relia.security.principal.PrincipalDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Optional<UUID> overrideAuditor = AuditContextHolder.getCurrentAuditor();
            if (overrideAuditor.isPresent()) {
                return overrideAuditor;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof PrincipalDetails principalDetails)) {
                return Optional.empty();
            }

            return Optional.ofNullable(principalDetails.getUser())
                    .map(user -> user.getId());
        };
    }
}
