/* Copyright the keycloak-twitch-identity-provider contributors. MIT License. */
package io.github.enhancerapp.keycloak.twitch;

import java.util.List;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/** Registers Twitch in Keycloak's native social provider list. */
public class TwitchIdentityProviderFactory extends AbstractIdentityProviderFactory<TwitchIdentityProvider>
        implements SocialIdentityProviderFactory<TwitchIdentityProvider> {
    public static final String PROVIDER_ID = "twitch";

    @Override public String getName() { return "Twitch"; }
    @Override public String getId() { return PROVIDER_ID; }
    @Override public TwitchIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new TwitchIdentityProvider(session, new TwitchIdentityProviderConfig(model));
    }
    @Override public TwitchIdentityProviderConfig createConfig() {
        TwitchIdentityProviderConfig config = new TwitchIdentityProviderConfig();
        config.applyDefaults();
        return config;
    }

    /** Endpoint fields are intentionally exposed: Twitch deployments may need proxy/test overrides. */
    @Override public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property().name("authorizationUrl").label("Authorization URL").type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(TwitchEndpoints.AUTHORIZATION_URL).add()
                .property().name("tokenUrl").label("Token URL").type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(TwitchEndpoints.TOKEN_URL).add()
                .property().name("userInfoUrl").label("Profile URL").type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(TwitchEndpoints.PROFILE_URL).add().build();
    }
}
