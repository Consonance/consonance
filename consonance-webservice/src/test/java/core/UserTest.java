package core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.consonance.webservice.core.ConsonanceUser;
import io.dropwizard.jackson.Jackson;
import org.junit.Test;

import java.sql.Timestamp;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dyuen
 */
public class UserTest {
        private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

        @Test
        public void serializesToJSON() throws Exception {
                final ConsonanceUser user = getUser();
                final String expected = MAPPER.writeValueAsString(
                        MAPPER.readValue(fixture("fixtures/user.json"), ConsonanceUser.class));
                assertThat(MAPPER.writeValueAsString(user)).isEqualTo(expected);
        }

        @Test
        public void deserializesFromJSON() throws Exception {
                final ConsonanceUser user = getUser();
                assertThat(MAPPER.readValue(fixture("fixtures/user.json"), ConsonanceUser.class))
                        .isEqualTo(user);
        }

        private ConsonanceUser getUser() {
                final ConsonanceUser user = new ConsonanceUser();
                user.setName("funky user");
                user.setHashedPassword("password");
                user.setCreateTimestamp(new Timestamp(0));
                user.setUpdateTimestamp(new Timestamp(0));
                user.setUserID(0);

                return user;
        }
}
