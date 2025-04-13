package backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String email = jwt.getClaimAsString("email");
        log.info("Email from JWT: {}", email);
        
        // Create a new JwtAuthenticationToken with the email as the principal name
        return new JwtAuthenticationToken(jwt, authorities, email);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Add default authorities
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        log.info("Added ROLE_USER authority");
        
        // Add any additional authorities from the token
        authorities.addAll(jwtGrantedAuthoritiesConverter.convert(jwt));
        
        log.info("Final authorities: {}", authorities);
        return authorities;
    }
} 