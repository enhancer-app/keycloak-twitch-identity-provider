# Keycloak Twitch Identity Provider

MIT-licensed Keycloak social provider for Twitch OAuth and Helix. It uses
`AbstractOAuth2IdentityProvider`, not Keycloak's OIDC provider, so Twitch token responses with
`"scope": ["openid", "user:read:email"]` work correctly.

## Requirements

- Java 17
- Keycloak **26.5.2**

## Build and test

```bash
./gradlew clean verify
```

The deployable thin provider JAR is `build/libs/keycloak-twitch.jar`.

## Installation

Copy the JAR to Keycloak exactly here:

```text
/opt/keycloak/providers/keycloak-twitch.jar
```

For optimized Keycloak deployments, run `kc.sh build` after installation, then start Keycloak.
For development, restart Keycloak after mounting or copying the JAR.

```bash
cp build/libs/keycloak-twitch.jar /opt/keycloak/providers/keycloak-twitch.jar
/opt/keycloak/bin/kc.sh build
```

The provider appears under **Identity Providers -> Add provider** as **Twitch**.

Docker Compose mount example:

```yaml
volumes:
  - ./providers/keycloak-twitch.jar:/opt/keycloak/providers/keycloak-twitch.jar:ro
```

## Twitch configuration

Create a Twitch application and set its redirect URI to:

```text
http://localhost:3010/realms/local/broker/twitch/endpoint
```

Production format:

```text
https://<keycloak-host>/realms/<realm>/broker/<alias>/endpoint
```

In Keycloak, add **Twitch**, retain alias `twitch`, then provide the Twitch **Client ID** and
**Client Secret**. Default scope is `user:read:email`; endpoints have Twitch defaults but remain
editable in the Admin Console.

## Behavior

- Authorization: `https://id.twitch.tv/oauth2/authorize`
- Token: `https://id.twitch.tv/oauth2/token`
- Profile: `https://api.twitch.tv/helix/users`
- Profile request headers: `Authorization: Bearer <token>` and `Client-Id: <client id>`.
- Stable federated subject: Twitch `data[0].id` only.
- Username/model username: `login`.
- Attributes: `twitch.id`, `twitch.login`, `twitch.display_name`,
  `twitch.profile_image_url`, and `picture` when an image is supplied.

The provider never links or identifies users by email, login, display name, or image. Keycloak
first-login/account-linking realm policy remains responsible for that behavior.

No authorization codes, tokens, client secrets, or raw Twitch responses are logged.

## Upgrade

This release is compiled and tested with Keycloak 26.5.2. Test a newer Keycloak release in staging
before deployment; do not bundle or override Keycloak dependencies in this JAR.
