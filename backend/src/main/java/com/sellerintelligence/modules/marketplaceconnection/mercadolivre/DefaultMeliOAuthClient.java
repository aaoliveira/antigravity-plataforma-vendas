package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import com.sellerintelligence.modules.marketplaceconnection.exception.MeliOAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Cliente HTTP real para o serviço OAuth 2.0 do Mercado Livre via Spring RestClient.
 */
@Component
public class DefaultMeliOAuthClient implements MeliOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultMeliOAuthClient.class);

    private final MeliOAuthProperties properties;
    private final RestClient restClient;

    public DefaultMeliOAuthClient(MeliOAuthProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String buildAuthorizationUrl(String state, String codeChallenge) {
        StringBuilder sb = new StringBuilder(properties.getAuthUrl());
        sb.append("?response_type=code");
        sb.append("&client_id=").append(URLEncoder.encode(properties.getAppId(), StandardCharsets.UTF_8));
        sb.append("&redirect_uri=").append(URLEncoder.encode(properties.getRedirectUri(), StandardCharsets.UTF_8));
        sb.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        sb.append("&code_challenge=").append(URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8));
        sb.append("&code_challenge_method=S256");

        return sb.toString();
    }

    @Override
    public MeliTokenResponse exchangeAuthorizationCode(String code, String codeVerifier, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", properties.getAppId());
        body.add("client_secret", properties.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", redirectUri != null && !redirectUri.isBlank() ? redirectUri : properties.getRedirectUri());
        body.add("code_verifier", codeVerifier);

        try {
            return restClient.post()
                    .uri(properties.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MeliTokenResponse.class);
        } catch (Exception e) {
            log.error("Falha ao trocar authorization code por token no Mercado Livre: {}", e.getMessage());
            throw new MeliOAuthException("Erro ao autenticar com o Mercado Livre: " + e.getMessage(), e);
        }
    }

    @Override
    public MeliTokenResponse refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", properties.getAppId());
        body.add("client_secret", properties.getClientSecret());
        body.add("refresh_token", refreshToken);

        try {
            return restClient.post()
                    .uri(properties.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MeliTokenResponse.class);
        } catch (Exception e) {
            log.error("Falha ao renovar token de acesso no Mercado Livre: {}", e.getMessage());
            throw new MeliOAuthException("Erro ao renovar sessão com o Mercado Livre: " + e.getMessage(), e);
        }
    }
}
