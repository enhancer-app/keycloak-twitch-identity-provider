/* Copyright the keycloak-twitch-identity-provider contributors. MIT License. */
package io.github.enhancerapp.keycloak.twitch;

import java.util.Locale;

/** Username presentation never changes the stable federated Twitch ID. */
public enum TwitchUsernameStrategy {
    STABLE_ID_PREFIX("stable-id-prefix"),
    PROVIDER_USERNAME("provider-username");

    public static final String DEFAULT_PREFIX = "twitch-";
    private final String configValue;

    TwitchUsernameStrategy(String configValue) { this.configValue = configValue; }
    public String getConfigValue() { return configValue; }

    public static TwitchUsernameStrategy fromConfigValue(String value) {
        if (value != null) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (TwitchUsernameStrategy strategy : values()) {
                if (strategy.configValue.equals(normalized) || strategy.name().equalsIgnoreCase(normalized)) return strategy;
            }
        }
        return STABLE_ID_PREFIX;
    }

    public String resolve(TwitchProfile profile, String prefix) {
        if (this == PROVIDER_USERNAME && profile.login() != null && !profile.login().isBlank()) return profile.login();
        return (prefix == null || prefix.isBlank() ? DEFAULT_PREFIX : prefix.trim()) + profile.id();
    }
}
