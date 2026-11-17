package br.com.sellerintelligence.infrastructure.security;

import br.com.sellerintelligence.infrastructure.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Set;

@Component
public class UrlSecurityValidator {

    private static final Set<String> FORBIDDEN_HOSTS = Set.of(
            "169.254.169.254",            // AWS / OpenStack metadata
            "metadata.google.internal",    // GCP metadata
            "100.100.100.200",             // Alibaba metadata
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "::1"
    );

    public void validateRedirectUri(String uriString, boolean allowLocalhostInDev) {
        if (uriString == null || uriString.isBlank()) {
            throw new BusinessException("INVALID_REDIRECT_URI", "Redirect URI cannot be empty", HttpStatus.BAD_REQUEST);
        }

        try {
            URI uri = URI.create(uriString);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new BusinessException("UNSAFE_REDIRECT_URI", "Scheme must be http or https", HttpStatus.BAD_REQUEST);
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new BusinessException("INVALID_REDIRECT_URI", "Host is required in redirect URI", HttpStatus.BAD_REQUEST);
            }

            String lowerHost = host.toLowerCase();

            if (allowLocalhostInDev && (lowerHost.equals("localhost") || lowerHost.equals("127.0.0.1"))) {
                return;
            }

            if (FORBIDDEN_HOSTS.contains(lowerHost) || lowerHost.startsWith("10.") || lowerHost.startsWith("192.168.")) {
                throw new BusinessException("PROHIBITED_REDIRECT_HOST", "Target host is prohibited for redirect (SSRF protection)", HttpStatus.BAD_REQUEST);
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_REDIRECT_URI", "Invalid URI syntax: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
