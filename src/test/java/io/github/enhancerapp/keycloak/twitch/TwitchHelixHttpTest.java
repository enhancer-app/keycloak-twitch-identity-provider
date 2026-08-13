package io.github.enhancerapp.keycloak.twitch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

/** Verifies the real HTTP request Twitch Helix receives, without contacting Twitch. */
class TwitchHelixHttpTest {
    private HttpServer server;
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> clientId = new AtomicReference<>();

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/helix/users", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            clientId.set(exchange.getRequestHeaders().getFirst("Client-Id"));
            byte[] body = "{\"data\":[{\"id\":\"42\",\"login\":\"twitch_user\"}]}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
        });
        server.start();
    }

    @AfterEach void stop() { server.stop(0); }

    @Test void sendsBothRequiredHelixHeaders() {
        HttpClientProvider http = mock(HttpClientProvider.class);
        when(http.getHttpClient()).thenReturn(HttpClients.createDefault());
        when(http.getMaxConsumedResponseSize()).thenReturn(HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getProvider(HttpClientProvider.class)).thenReturn(http);
        IdentityProviderModel model = new IdentityProviderModel();
        model.setAlias("twitch");
        model.setConfig(new HashMap<>());
        model.getConfig().put("clientId", "twitch-client-id");
        model.getConfig().put("userInfoUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/helix/users");
        TwitchIdentityProvider provider = new TwitchIdentityProvider(session, new TwitchIdentityProviderConfig(model));

        BrokeredIdentityContext user = provider.doGetFederatedIdentity("twitch-access-token");

        assertThat(user.getId()).isEqualTo("42");
        assertThat(authorization.get()).isEqualTo("Bearer twitch-access-token");
        assertThat(clientId.get()).isEqualTo("twitch-client-id");
    }
}
