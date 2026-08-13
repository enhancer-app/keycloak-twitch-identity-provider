/* Copyright the keycloak-twitch-identity-provider contributors. MIT License. */
package io.github.enhancerapp.keycloak.twitch;

import com.fasterxml.jackson.databind.JsonNode;

/** Parses the Helix users envelope without exposing upstream payloads in error messages. */
public final class TwitchProfileParser {
    public static final String EMPTY_DATA = "Twitch did not return a user profile.";
    public static final String MISSING_ID = "Twitch returned a user profile without a stable account ID.";
    public static final String MISSING_LOGIN = "Twitch returned a user profile without a login name.";

    private TwitchProfileParser() {
    }

    public static TwitchProfile parse(JsonNode root) {
        JsonNode data = root == null ? null : root.get("data");
        if (data == null || !data.isArray() || data.isEmpty() || !data.get(0).isObject()) {
            throw new TwitchProfileException(EMPTY_DATA);
        }
        JsonNode user = data.get(0);
        String id = text(user, "id");
        if (id == null) throw new TwitchProfileException(MISSING_ID);
        String login = text(user, "login");
        if (login == null) throw new TwitchProfileException(MISSING_LOGIN);
        return new TwitchProfile(id, login, text(user, "display_name"), text(user, "email"), text(user, "profile_image_url"));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) return null;
        return value.asText().trim();
    }

    public static final class TwitchProfileException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        TwitchProfileException(String message) {
            super(message);
        }
    }
}
