/* Copyright the keycloak-twitch-identity-provider contributors. MIT License. */
package io.github.enhancerapp.keycloak.twitch;

import java.io.IOException;

import org.apache.http.client.config.RequestConfig;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import com.fasterxml.jackson.databind.JsonNode;

/** Twitch OAuth provider that deliberately avoids Keycloak's OIDC token deserializer. */
public class TwitchIdentityProvider extends AbstractOAuth2IdentityProvider<TwitchIdentityProviderConfig>
        implements SocialIdentityProvider<TwitchIdentityProviderConfig> {
    static final Logger LOG = Logger.getLogger(TwitchIdentityProvider.class);
    public static final String DEFAULT_SCOPE = "user:read:email";
    static final String FAILURE = "Could not retrieve the Twitch user profile. Check the provider configuration and server log.";
    private static final RequestConfig TIMEOUTS = RequestConfig.custom()
            .setConnectTimeout(5_000).setSocketTimeout(5_000).setConnectionRequestTimeout(5_000).build();

    public TwitchIdentityProvider(KeycloakSession session, TwitchIdentityProviderConfig config) {
        super(session, config);
        config.applyDefaults();
        if (config.isStoreToken() == null) config.setStoreToken(false);
    }

    @Override
    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }

    /**
     * Reads only access_token from a Twitch response. Twitch represents scope as a JSON array,
     * unlike Keycloak's OIDC AccessTokenResponse; this provider never deserializes that OIDC type.
     */
    @Override
    public BrokeredIdentityContext getFederatedIdentity(String tokenResponse) {
        try {
            JsonNode token = mapper.readTree(tokenResponse);
            JsonNode access = token == null ? null : token.get(OAUTH2_PARAMETER_ACCESS_TOKEN);
            if (access == null || !access.isTextual() || access.asText().isBlank()) {
                LOG.warn("Twitch token response did not contain an access token.");
                throw new IdentityBrokerException(FAILURE);
            }
        } catch (IdentityBrokerException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Twitch token response was not valid JSON.");
            throw new IdentityBrokerException(FAILURE);
        }
        return super.getFederatedIdentity(tokenResponse);
    }

    protected SimpleHttp createHttp() {
        return SimpleHttp.create(session).withRequestConfig(TIMEOUTS).withMaxConsumedResponseSize(256L * 1024L);
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) throw new IdentityBrokerException(FAILURE);
        final JsonNode profile;
        try (SimpleHttpResponse response = createHttp().doGet(getConfig().getUserInfoUrl())
                .header("Authorization", "Bearer " + accessToken)
                .header("Client-Id", getConfig().getClientId())
                .header("Accept", "application/json").asResponse()) {
            if (response.getStatus() < 200 || response.getStatus() > 299) {
                LOG.warnf("Twitch profile endpoint returned HTTP %d.", response.getStatus());
                throw new IdentityBrokerException(FAILURE);
            }
            profile = response.asJson();
        } catch (IdentityBrokerException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            LOG.warnf("Twitch profile endpoint failed (%s).", e.getClass().getSimpleName());
            throw new IdentityBrokerException(FAILURE);
        }
        return extractIdentityFromProfile(null, profile);
    }

    @Override
    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profileJson) {
        final TwitchProfile profile;
        try {
            profile = TwitchProfileParser.parse(profileJson);
        } catch (TwitchProfileParser.TwitchProfileException e) {
            LOG.warnf("Rejected Twitch profile: %s", e.getMessage());
            throw new IdentityBrokerException(e.getMessage());
        }
        BrokeredIdentityContext user = new BrokeredIdentityContext(profile.id(), getConfig());
        user.setUsername(getConfig().getUsernameStrategy().resolve(profile, getConfig().getUsernamePrefix()));
        user.setModelUsername(user.getUsername());
        user.setEmail(profile.email());
        user.setUserAttribute("twitch.id", profile.id());
        user.setUserAttribute("twitch.login", profile.login());
        setOptional(user, "twitch.display_name", profile.displayName());
        setOptional(user, "twitch.email", profile.email());
        setOptional(user, "twitch.profile_image_url", profile.profileImageUrl());
        user.setIdp(this);
        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profileJson, getConfig().getAlias());
        return user;
    }

    private static void setOptional(BrokeredIdentityContext context, String name, String value) {
        if (value != null) context.setUserAttribute(name, value);
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, BrokeredIdentityContext context) {
        synchronizeProfile(user, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, BrokeredIdentityContext context) {
        synchronizeProfile(user, context);
    }

    private static void synchronizeProfile(UserModel user, BrokeredIdentityContext context) {
        user.setEmail(context.getEmail());
        synchronizeAttribute(user, context, "twitch.id");
        synchronizeAttribute(user, context, "twitch.login");
        synchronizeAttribute(user, context, "twitch.display_name");
        synchronizeAttribute(user, context, "twitch.email");
        synchronizeAttribute(user, context, "twitch.profile_image_url");
        user.removeAttribute("picture");
    }

    private static void synchronizeAttribute(UserModel user, BrokeredIdentityContext context, String name) {
        String value = context.getUserAttribute(name);
        if (value == null || value.isBlank()) user.removeAttribute(name);
        else user.setSingleAttribute(name, value);
    }
}
