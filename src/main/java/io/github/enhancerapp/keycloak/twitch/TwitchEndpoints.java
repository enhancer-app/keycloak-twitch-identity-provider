/* Copyright the keycloak-twitch-identity-provider contributors. MIT License. */
package io.github.enhancerapp.keycloak.twitch;

/** Default Twitch OAuth and Helix endpoints. Administrators may override them. */
public final class TwitchEndpoints {
    public static final String AUTHORIZATION_URL = "https://id.twitch.tv/oauth2/authorize";
    public static final String TOKEN_URL = "https://id.twitch.tv/oauth2/token";
    public static final String PROFILE_URL = "https://api.twitch.tv/helix/users";

    private TwitchEndpoints() {
    }
}
