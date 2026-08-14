# Keycloak Twitch Identity Provider

[![CI](https://github.com/enhancer-app/keycloak-twitch-identity-provider/actions/workflows/ci.yml/badge.svg)](https://github.com/enhancer-app/keycloak-twitch-identity-provider/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A standalone, production-oriented [Keycloak](https://www.keycloak.org/) social identity provider
for [Twitch](https://www.twitch.tv/) OAuth and Helix. It adds **Twitch** to Keycloak Admin
Console's native social provider list, so administrators configure normal identity-provider
settings instead of writing custom broker code.

This is a generic Keycloak extension. It has no application-specific realm, UI, database,
deployment topology, backend service, credential, or hosted-service assumption.

> Twitch is a trademark of its respective owner. This independent project is not affiliated with
> or endorsed by Twitch.

## Features

- OAuth authorization-code flow through `AbstractOAuth2IdentityProvider`, not
  `OIDCIdentityProvider`.
- Correctly accepts Twitch token responses whose `scope` is a JSON array, avoiding Keycloak's OIDC
  string-only scope deserialization.
- Native Keycloak social-provider registration (`Twitch` in Admin Console).
- Default Twitch endpoints and `user:read:email` scope, editable by administrators.
- Standard Keycloak PKCE configuration, first-login flow, sync modes and token-storage behavior.
- Stable federated identity subject from only Twitch Helix `data[0].id`.
- Required Helix profile headers: bearer access token and configured `Client-Id`.
- Strict profile and HTTP validation, bounded timeouts, response-size limit, and payload-safe logs.
- Thin provider JAR: no bundled Keycloak, Jackson, HTTP client or logging libraries.
- Optional login theme with an accessible generic provider icon.

## Compatibility

| Requirement | Version |
| --- | --- |
| Java runtime / build toolchain | 17 |
| Keycloak compile and smoke-test target | **26.5.2** |
| Provider artifact | `io.github.enhancer-app:keycloak-twitch-identity-provider` |
| Release provider JAR | `keycloak-twitch-identity-provider-<version>.jar` |

Keycloak 26.5.2 is the only guaranteed compatibility target. Newer versions may work, but must
pass smoke and real-login testing before deployment. See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Install

1. Download the versioned provider JAR, `keycloak-twitch-identity-provider-<version>.jar`, from a GitHub release or build it locally.
2. Copy it exactly to `/opt/keycloak/providers/keycloak-twitch.jar`.
3. For optimized Keycloak deployments, run `kc.sh build`.
4. Start or restart Keycloak.

```bash
cp keycloak-twitch-identity-provider-<version>.jar /opt/keycloak/providers/keycloak-twitch.jar
/opt/keycloak/bin/kc.sh build
/opt/keycloak/bin/kc.sh start
```

For development, restart Keycloak after mounting or copying the JAR. Open **Identity Providers →
Add provider** in the target realm; **Twitch** should be available. If it is not, see
[Troubleshooting](#troubleshooting).

## Docker

The included Dockerfile uses `quay.io/keycloak/keycloak:26.5.2`, installs the JAR at the required
path, and runs Keycloak augmentation:

```bash
./gradlew clean prepareDockerProvider
docker build -t keycloak-twitch:local .
docker run --rm -p 3010:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD='<choose-a-local-password>' \
  keycloak-twitch:local start-dev
```

For a development Keycloak service, mount the JAR read-only:

```yaml
volumes:
  - ./providers/keycloak-twitch.jar:/opt/keycloak/providers/keycloak-twitch.jar:ro
```

The provided `docker-compose.yml` is only an example. It contains no realm import, OAuth client,
secret, database, or production deployment assumptions.

## Configure Twitch

Create a Twitch application and register the Keycloak broker callback URI exactly.

Local example:

```text
http://localhost:3010/realms/local/broker/twitch/endpoint
```

Production format:

```text
https://<keycloak-host>/realms/<realm>/broker/<alias>/endpoint
```

The URI must match byte-for-byte during authorization and token exchange. A different scheme,
host, port, realm, alias, path, or trailing slash makes Twitch reject the flow.

In **Identity Providers → Twitch**, retain alias `twitch` unless you deliberately use another
stable alias, then configure:

| Setting | Default | Notes |
| --- | --- | --- |
| Client ID | required | Issued by your Twitch application; sent as required `Client-Id` Helix header. |
| Client Secret | required | Managed by Keycloak; never commit it. |
| Display name | `Twitch` / Keycloak default | Login button label. |
| Authorization URL | Twitch production endpoint | Editable for controlled testing/proxy deployments. |
| Token URL | Twitch production endpoint | Editable for controlled testing/proxy deployments. |
| Profile URL | Twitch Helix users endpoint | Must return the documented Helix users envelope. |
| Default scopes | `user:read:email` | Required to receive email when Twitch supplies it. |
| PKCE | Keycloak setting | Enabled and configured through standard Keycloak OAuth provider configuration. |
| Store tokens | off | Standard Keycloak broker storage control; enable only when required. |

Default endpoints:

```text
Authorization: https://id.twitch.tv/oauth2/authorize
Token:         https://id.twitch.tv/oauth2/token
Profile:       https://api.twitch.tv/helix/users
```

## OAuth behavior

Twitch returns token responses that can contain either of these legal `scope` forms:

```json
{ "access_token": "...", "scope": ["openid", "user:read:email"] }
```

```json
{ "access_token": "...", "scope": "user:read:email" }
```

Keycloak's generic OIDC provider expects a string and fails on the array form. This extension uses
`AbstractOAuth2IdentityProvider`, extracts and validates only `access_token`, and delegates the
ordinary authorization-code exchange and optional token storage to Keycloak. No custom account
linking by email is implemented.

The provider requests the current user from Helix with:

```http
GET /helix/users HTTP/1.1
Authorization: Bearer <access-token>
Client-Id: <configured-client-id>
Accept: application/json
```

## Profile mapping and identity

Twitch returns a user in `data[0]`:

```json
{
  "data": [{
    "id": "141981764",
    "login": "twitchdev",
    "display_name": "TwitchDev",
    "email": "user@example.com",
    "profile_image_url": "https://..."
  }]
}
```

| Twitch field | Keycloak use | Identity-critical? |
| --- | --- | --- |
| `data[0].id` | Federated identity subject / broker user ID; `twitch.id` attribute | **Yes. The only identity key.** |
| `data[0].login` | `twitch.login`; source for optional `provider-username` strategy | No; profile data. |
| `data[0].display_name` | `twitch.display_name` attribute | No; mutable. |
| `data[0].email` | Keycloak email and `twitch.email` when supplied | No; optional. |
| `data[0].profile_image_url` | `twitch.profile_image_url` when supplied | No; optional and mutable. |

The provider never identifies or links accounts using email, login, display name, or avatar. Realm
first-login and account-linking policies remain entirely under Keycloak administrator control.

## Provider icon

The JAR includes an opt-in login theme named `twitch`. Select it under **Realm Settings → Themes →
Login Theme** to display a generic provider-button mark for the default `twitch` alias. It does not
ship or claim an official Twitch trademark asset.

For an existing custom login theme, add this property and copy the CSS/SVG resources from
`src/main/resources/theme/twitch`:

```properties
kcLogoIdP-twitch=kc-social-twitch
```

For another alias, use `kcLogoIdP-<alias>`.

## Security model

- The provider validates a non-blank textual access token before using it.
- Profile calls have 5-second connection, socket and connection-pool timeouts plus a 256 KiB body
  limit.
- Non-2xx responses, malformed JSON, absent/non-array/empty `data`, non-object `data[0]`, missing
  Twitch ID, and missing login fail closed with safe Keycloak errors.
- Only `data[0].id` establishes the federated identity.
- Provider logs never contain authorization codes, client secrets, access tokens, refresh tokens,
  or raw Twitch token/profile responses.
- The JAR contains no Keycloak/Jackson/HTTP/logging runtime dependencies.

Report vulnerabilities privately as described in [SECURITY.md](SECURITY.md).

## Local development

Prerequisites: JDK 17. Docker is needed for the smoke test and Docker image build. The Gradle
Wrapper downloads Gradle; no global installation is needed.

```bash
./gradlew clean verify
./gradlew test -PwithSmoke
docker build -t keycloak-twitch:local .
```

The smoke test starts `quay.io/keycloak/keycloak:26.5.2` with the built JAR and verifies provider
discovery. Use [docs/MANUAL_E2E.md](docs/MANUAL_E2E.md) for a real Twitch application.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Twitch is absent from **Add provider** | Confirm the JAR path, run `kc.sh build` for optimized Keycloak, restart, and inspect startup logs. |
| Twitch rejects `redirect_uri` | Compare the registered URI and broker callback byte-for-byte. |
| Login returns from Twitch but fails | Confirm client ID/secret, scope, and connectivity to `id.twitch.tv` / `api.twitch.tv`; never paste token responses into logs or issues. |
| Helix returns 401/403 | Ensure the token is a user access token and that `Client-Id` matches the configured Twitch application. |
| Email is absent | Expected unless Twitch returns it for the user token and `user:read:email` is granted. |
| Wrong user was linked | Inspect the federated identity link. The provider subject must equal Twitch `data[0].id`, never email/login/display name. |
| Icon is missing | Select the `twitch` login theme or configure `kcLogoIdP-<alias>` in the active custom theme. |

## Upgrade guidance

1. Review [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).
2. Back up realms and federated identity links.
3. Test the target Keycloak image and new provider JAR in staging.
4. Run `./gradlew test -PwithSmoke` and the manual Twitch login test.
5. Replace the JAR, run `kc.sh build` for optimized deployments, restart, and verify **Twitch** in
   the Admin Console.
6. Keep the provider alias stable. Changing it changes the callback URI and may require updating
   the Twitch application and reviewing provider links.

## License

Licensed under the [MIT License](LICENSE).
