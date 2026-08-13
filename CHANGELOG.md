# Changelog

All notable changes are documented here.

## Unreleased

### Added

- Initial Twitch Keycloak social identity provider.
- OAuth authorization-code flow using `AbstractOAuth2IdentityProvider` to handle Twitch `scope`
  arrays correctly.
- Validated Twitch Helix profile retrieval with required headers and stable identity mapping.
- Unit tests, Keycloak 26.5.2 smoke discovery test, Docker build, CI, release checksum and manual
  end-to-end testing documentation.
