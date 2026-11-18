package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Propriedades de configuração do fluxo OAuth 2.0 do Mercado Livre.
 */
@Component
@ConfigurationProperties(prefix = "mercadolivre.oauth")
public class MeliOAuthProperties {

    private String appId = "test-app-id";
    private String clientSecret = "test-client-secret";
    private String redirectUri = "http://localhost:3000/api/v1/integrations/mercadolivre/callback";
    private String authUrl = "https://auth.mercadolivre.com.br/authorization";
    private String tokenUrl = "https://api.mercadolibre.com/oauth/token";
    private List<String> scopes = new ArrayList<>(List.of("read", "offline_access"));

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes != null ? new ArrayList<>(scopes) : new ArrayList<>(List.of("read", "offline_access"));
    }
}
