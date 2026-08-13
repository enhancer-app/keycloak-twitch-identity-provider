package io.github.enhancerapp.keycloak.twitch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/** Boots Keycloak 26.5.2 with the built JAR and verifies provider discovery at startup. */
@Tag("smoke")
class TwitchProviderDiscoverySmokeTest {
    @Test void keycloakLoadsTwitchProvider() {
        Path jar = Path.of(System.getProperty("twitch.providerJar"));
        assertThat(jar).exists();
        String version = System.getProperty("twitch.keycloakVersion", "26.5.2");
        try (GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:" + version)
                .withCopyFileToContainer(MountableFile.forHostPath(jar), "/opt/keycloak/providers/keycloak-twitch.jar")
                .withExposedPorts(8080)
                .withCommand("start-dev")
                .waitingFor(Wait.forHttp("/realms/master/.well-known/openid-configuration")
                        .forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)))) {
            keycloak.start();
            // Keycloak reaches this point only after build-time augmentation scanned the JAR.
            assertThat(keycloak.getLogs())
                    .contains("TwitchIdentityProviderFactory")
                    .contains("Listening on:");
        }
    }
}
