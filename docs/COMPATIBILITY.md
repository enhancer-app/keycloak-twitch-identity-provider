# Compatibility

## Supported Keycloak version

| Provider release line | Compile target | Guaranteed Keycloak version | Tested image |
| --- | --- | --- | --- |
| `1.x` | `26.5.2` | `26.5.2` | `quay.io/keycloak/keycloak:26.5.2` |

This provider is built with Java 17. It uses Keycloak provider SPI APIs and the
Keycloak-provided `org.keycloak.http.simple` HTTP API.

## Version policy

- Keycloak 26.5.2 is the only version guaranteed by the release line because CI compiles and
  runs the container smoke test against that exact image.
- A newer Keycloak release may work, but is not a compatibility claim until its SPI linkage,
  provider discovery, OAuth flow and real Twitch login are tested in staging.
- This project does not use reflection or compatibility shims to bridge binary API changes. A
  source-incompatible future Keycloak release gets a new provider release line.
- Keycloak 26.0–26.4 are unsupported because their HTTP SPI packaging differs from 26.5.2.

## Theme compatibility

The optional `twitch` login theme extends `keycloak.v2` and includes
`css/styles.css css/twitch-idp.css`. If Keycloak changes its parent stylesheet layout, update the
theme properties and manually verify the icon. The provider itself has no runtime dependency on
the theme.
