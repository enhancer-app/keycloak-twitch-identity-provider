package io.github.enhancerapp.keycloak.twitch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class TwitchIdentityProviderTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final class ExposedProvider extends TwitchIdentityProvider {
        ExposedProvider() {
            super(mock(KeycloakSession.class), config());
        }
        BrokeredIdentityContext map(JsonNode node) { return extractIdentityFromProfile((EventBuilder) null, node); }
        @Override protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
            return new BrokeredIdentityContext("test-subject", getConfig());
        }
    }

    private static TwitchIdentityProviderConfig config() {
        IdentityProviderModel model = new IdentityProviderModel();
        model.setAlias("twitch");
        model.setConfig(new HashMap<>());
        return new TwitchIdentityProviderConfig(model);
    }

    @Test void acceptsTokenScopeAsArrayWithoutOidcDeserialization() {
        BrokeredIdentityContext result = new ExposedProvider().getFederatedIdentity(
                "{\"access_token\":\"token\",\"scope\":[\"openid\",\"user:read:email\"]}");
        assertThat(result.getId()).isEqualTo("test-subject");
    }

    @Test void acceptsTokenScopeAsString() {
        BrokeredIdentityContext result = new ExposedProvider().getFederatedIdentity(
                "{\"access_token\":\"token\",\"scope\":\"user:read:email\"}");
        assertThat(result.getId()).isEqualTo("test-subject");
    }

    @Test void mapsTwitchProfileToStableSubjectAndAttributes() throws Exception {
        BrokeredIdentityContext user = new ExposedProvider().map(JSON.readTree("""
            {"data":[{"id":"7","login":"streamer","display_name":"Streamer",
            "email":"a@example.test","profile_image_url":"https://cdn.example/a.png"}]}"""));
        assertThat(user.getId()).isEqualTo("7");
        assertThat(user.getUsername()).isEqualTo("twitch-7");
        assertThat(user.getModelUsername()).isEqualTo("twitch-7");
        assertThat(user.getEmail()).isEqualTo("a@example.test");
        assertThat(user.getUserAttribute("twitch.id")).isEqualTo("7");
        assertThat(user.getUserAttribute("twitch.login")).isEqualTo("streamer");
        assertThat(user.getUserAttribute("twitch.display_name")).isEqualTo("Streamer");
        assertThat(user.getUserAttribute("twitch.profile_image_url")).isEqualTo("https://cdn.example/a.png");
        assertThat(user.getUserAttribute("twitch.email")).isEqualTo("a@example.test");
    }

    @Test void omitsOptionalMappingsWhenTwitchDoesNotSupplyThem() throws Exception {
        BrokeredIdentityContext user = new ExposedProvider().map(JSON.readTree("{\"data\":[{\"id\":\"7\",\"login\":\"streamer\"}]}"));
        assertThat(user.getEmail()).isNull();
        assertThat(user.getUserAttribute("twitch.profile_image_url")).isNull();
    }
}
