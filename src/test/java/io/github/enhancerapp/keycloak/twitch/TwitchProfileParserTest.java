package io.github.enhancerapp.keycloak.twitch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TwitchProfileParserTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test void parsesValidHelixDataZero() throws Exception {
        TwitchProfile profile = TwitchProfileParser.parse(json.readTree("""
            {"data":[{"id":"141981764","login":"twitchdev","display_name":"TwitchDev",
            "email":"user@example.test","profile_image_url":"https://cdn.example/avatar.png"}]}"""));
        assertThat(profile.id()).isEqualTo("141981764");
        assertThat(profile.login()).isEqualTo("twitchdev");
        assertThat(profile.email()).isEqualTo("user@example.test");
    }

    @Test void rejectsEmptyData() throws Exception {
        assertThatThrownBy(() -> TwitchProfileParser.parse(json.readTree("{\"data\":[]}")))
                .hasMessage(TwitchProfileParser.EMPTY_DATA);
    }

    @Test void rejectsMissingStableId() throws Exception {
        assertThatThrownBy(() -> TwitchProfileParser.parse(json.readTree("{\"data\":[{\"login\":\"twitchdev\"}]}")))
                .hasMessage(TwitchProfileParser.MISSING_ID);
    }

    @Test void keepsOptionalEmailAndImageAbsent() throws Exception {
        TwitchProfile profile = TwitchProfileParser.parse(json.readTree("{\"data\":[{\"id\":\"1\",\"login\":\"a\"}]}"));
        assertThat(profile.email()).isNull();
        assertThat(profile.profileImageUrl()).isNull();
    }
}
