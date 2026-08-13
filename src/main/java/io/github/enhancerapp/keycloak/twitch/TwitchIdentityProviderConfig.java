/* Copyright the keycloak-twitch-identity-provider contributors. MIT License. */
package io.github.enhancerapp.keycloak.twitch;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

/** Configuration defaults for Twitch. Endpoint and scope settings remain administrator-editable. */
public class TwitchIdentityProviderConfig extends OAuth2IdentityProviderConfig {
    private static final long serialVersionUID = 1L;

    public TwitchIdentityProviderConfig() {
        super();
    }

    public TwitchIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public void applyDefaults() {
        if (getAuthorizationUrl() == null || getAuthorizationUrl().isBlank()) setAuthorizationUrl(TwitchEndpoints.AUTHORIZATION_URL);
        if (getTokenUrl() == null || getTokenUrl().isBlank()) setTokenUrl(TwitchEndpoints.TOKEN_URL);
        if (getUserInfoUrl() == null || getUserInfoUrl().isBlank()) setUserInfoUrl(TwitchEndpoints.PROFILE_URL);
        if (getDefaultScope() == null || getDefaultScope().isBlank()) setDefaultScope(TwitchIdentityProvider.DEFAULT_SCOPE);
    }
}
