# Manual End-to-End Test

Use a non-production Keycloak realm and a dedicated Twitch developer application. Never commit
the application's credentials, authorization codes, tokens or real profile data.

## 1. Build and start Keycloak

```bash
./gradlew clean prepareDockerProvider
docker build -t keycloak-twitch:local .
docker run --rm -p 3010:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD='<choose-a-local-password>' \
  keycloak-twitch:local start-dev
```

Create a realm named `local` or substitute your own realm in the following instructions.

## 2. Configure Twitch

In Twitch Developer Console, create/open a test application and register:

```text
http://localhost:3010/realms/local/broker/twitch/endpoint
```

For a deployed Keycloak instance use:

```text
https://<keycloak-host>/realms/<realm>/broker/<alias>/endpoint
```

## 3. Configure Keycloak

1. Open **Identity Providers** in the target realm.
2. Select **Twitch** from **Add provider**.
3. Keep alias `twitch`, unless deliberately changing both the callback registration and alias.
4. Enter Twitch Client ID and Client Secret.
5. Keep scope `user:read:email`, unless a deliberate scope policy requires more.
6. Save with **Store Tokens** disabled unless another Keycloak integration requires upstream
   token storage.

## 4. Verify login

1. Open a client login page and select Twitch.
2. Confirm the redirect uses `https://id.twitch.tv/oauth2/authorize` and standard code flow.
3. Authenticate with the test Twitch account and consent.
4. Confirm Keycloak returns to the application through its standard first-login flow.
5. Inspect the user’s provider link: subject must be Twitch `data[0].id`.
6. Inspect attributes `twitch.id`, `twitch.login`, `twitch.display_name`,
   `twitch.email` and `twitch.profile_image_url` where Twitch supplied values.

## 5. Negative checks

- Alter the registered callback URI: Twitch should deny the flow.
- Use an invalid client secret: error pages/logs must not reveal it or raw Twitch responses.
- Verify the Twitch Helix request is authorized for the configured client; a mismatched `Client-Id`
  should be rejected by Twitch without exposing the token.

## Cleanup

Delete the test realm and revoke/delete the test Twitch application or its credentials.
