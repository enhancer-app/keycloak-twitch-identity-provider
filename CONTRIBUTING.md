# Contributing

## Prerequisites

- JDK 17
- Docker, only for `-PwithSmoke` and Docker image verification

Use the included Gradle Wrapper; a globally installed Gradle is not required.

## Validate a change

```bash
./gradlew clean verify
./gradlew test -PwithSmoke
docker build -t keycloak-twitch:local .
```

Keep this a thin, generic Keycloak provider. Do not add runtime copies of Keycloak/Jackson/HTTP
libraries, databases, realm imports, environment-specific configuration, client credentials, real
tokens or private infrastructure. New upstream parsing must fail closed and never log raw data.

## Compatibility changes

Changing Keycloak dependencies, SPI usage, theme contracts or the supported target requires an
update to [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) and a Keycloak 26.5.2 smoke test.
