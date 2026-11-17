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

    private static final Set<String> ALLOWED_POLICY_DOMAINS = Set.of(
            "mercadolivre.com.br",
            "mercadolibre.com",
            "auth.mercadolivre.com.br",
            "developers.mercadolivre.com.br",
            "open.shopee.com",
            "shopee.com.br",
            "partner.shopeemobile.com",
            "developer-docs.amazon.com",
            "amazon.com.br",
            "amazon.com",
            "sellercentral.amazon.com.br",
            "partner.tiktokshop.com",
            "tiktokglobalshop.com",
            "developers.magazineluiza.com.br",
            "magazineluiza.com.br",
            "open.shein.com",
            "shein.com"
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

    public void validatePolicyMonitoringUrl(String uriString) {
        if (uriString == null || uriString.isBlank()) {
            throw new BusinessException("INVALID_POLICY_URL", "Policy URL cannot be empty", HttpStatus.BAD_REQUEST);
        }

        try {
            URI uri = URI.create(uriString);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new BusinessException("UNSAFE_POLICY_URL", "Policy URL scheme must be http or https", HttpStatus.BAD_REQUEST);
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new BusinessException("INVALID_POLICY_URL", "Host is required in policy URL", HttpStatus.BAD_REQUEST);
            }

            String lowerHost = host.toLowerCase();

            // Strict anti-SSRF checks
            if (FORBIDDEN_HOSTS.contains(lowerHost) || lowerHost.startsWith("10.")
                    || lowerHost.startsWith("192.168.") || lowerHost.startsWith("172.")
                    || lowerHost.contains("internal") || lowerHost.contains("local")) {
                throw new BusinessException("SSRF_VIOLATION_BLOCKED",
                        "Target host " + lowerHost + " is prohibited for policy monitoring (SSRF protection)", HttpStatus.BAD_REQUEST);
            }

            // Must match authorized official domains
            boolean domainAllowed = ALLOWED_POLICY_DOMAINS.stream()
                    .anyMatch(allowed -> lowerHost.equals(allowed) || lowerHost.endsWith("." + allowed));

            if (!domainAllowed) {
                throw new BusinessException("UNAUTHORIZED_POLICY_DOMAIN",
                        "Host " + lowerHost + " is not an authorized official marketplace policy domain", HttpStatus.BAD_REQUEST);
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_POLICY_URL", "Invalid URI syntax: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
