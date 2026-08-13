# Security Policy

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability. Report it privately through GitHub
Security Advisories:

<https://github.com/enhancer-app/keycloak-twitch-identity-provider/security/advisories/new>

Include affected provider and Keycloak versions, deployment assumptions and a minimal proof of
concept. Never include real client secrets, authorization codes, access tokens, refresh tokens or
Twitch profile data.

## Supported version

The `1.x` line is compiled and smoke-tested against Keycloak **26.5.2**. Security fixes target the
latest release line. See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Security constraints

- Federated identity uses only Twitch Helix `data[0].id`.
- Helix requests use bounded timeouts and require both bearer token and `Client-Id` headers.
- Token/profile failures use safe messages. The provider never logs secrets, tokens, authorization
  codes or raw remote response bodies.
- The provider remains thin and bundles no Keycloak, Jackson, HTTP, or logging library.
