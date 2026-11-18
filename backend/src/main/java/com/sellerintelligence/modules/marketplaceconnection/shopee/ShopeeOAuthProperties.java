package com.sellerintelligence.modules.marketplaceconnection.shopee;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriedades de configuração para integração oficial com a Shopee Open Platform.
 */
@Component
@ConfigurationProperties(prefix = "shopee.open-platform")
public class ShopeeOAuthProperties {

    private long partnerId = 0L;
    private String partnerKey = "";
    private String redirectUrl = "http://localhost:3000/api/v1/integrations/shopee/callback";
    private String authBaseUrl = "https://partner.shopeemobile.com";
    private String apiBaseUrl = "https://partner.shopeemobile.com";
    private String webhookUrl = "http://localhost:8082/api/v1/integrations/shopee/webhook";
    private boolean sandbox = false;
    private boolean enabled = true;

    public long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(long partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerKey() {
        return partnerKey;
    }

    public void setPartnerKey(String partnerKey) {
        this.partnerKey = partnerKey;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getAuthBaseUrl() {
        return authBaseUrl;
    }

    public void setAuthBaseUrl(String authBaseUrl) {
        this.authBaseUrl = authBaseUrl;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
