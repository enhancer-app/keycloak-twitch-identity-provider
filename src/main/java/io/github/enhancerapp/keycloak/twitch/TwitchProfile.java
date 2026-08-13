/* Copyright the keycloak-twitch-identity-provider contributors. MIT License. */
package io.github.enhancerapp.keycloak.twitch;

/** Validated current-user record from Twitch Helix. */
public record TwitchProfile(String id, String login, String displayName, String email, String profileImageUrl) {
}
