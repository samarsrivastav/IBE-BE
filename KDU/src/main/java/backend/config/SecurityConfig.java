package backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import java.util.List;
import java.util.function.Predicate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain...");
        log.info("Using issuer URI: {}", issuerUri);
        
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> {
                log.info("Configuring authorization rules...");
                auth
                    .requestMatchers("/api/bookings/my-bookings").authenticated()
                    .anyRequest().permitAll();
            })
            .oauth2ResourceServer(oauth2 -> {
                log.info("Configuring OAuth2 resource server...");
                oauth2.jwt(jwt -> {
                    log.info("Configuring JWT...");
                    jwt.decoder(jwtDecoder());
                    jwt.jwtAuthenticationConverter(new JwtRoleConverter());
                });
            })
            .sessionManagement(session -> {
                log.info("Configuring session management...");
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            });

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("Creating JWT decoder with issuer URI: {}", issuerUri);
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuerUri)
                .build();
        
        // Create validators with detailed logging
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
        JwtIssuerValidator issuerValidator = new JwtIssuerValidator(issuerUri);
        
        // Add custom claim validator to log token claims
        Predicate<Jwt> tokenValidator = token -> {
            try {
                log.info("=== JWT Token Validation Details ===");
                log.info("Token issuer: {}", token.getIssuer());
                log.info("Token subject: {}", token.getSubject());
                log.info("Token audience: {}", token.getAudience());
                
                // Format dates in a readable way
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                    .withZone(ZoneId.systemDefault());
                
                Instant now = Instant.now();
                Instant exp = token.getExpiresAt();
                Instant iat = token.getIssuedAt();
                
                log.info("Current time: {}", formatter.format(now));
                log.info("Token issued at: {}", formatter.format(iat));
                log.info("Token expires at: {}", formatter.format(exp));
                log.info("Time until expiration: {} seconds", exp.getEpochSecond() - now.getEpochSecond());
                
                log.info("Token claims: {}", token.getClaims());
                log.info("=== End JWT Token Validation Details ===");
                
                return true;
            } catch (Exception e) {
                log.error("Error validating token: {}", e.getMessage());
                return false;
            }
        };
        
        // Set validators with error handling
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        
        return jwtDecoder;
    }
} 