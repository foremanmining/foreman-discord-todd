package mn.foreman.discordbot.db;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.Instant;

/**
 * A {@link PrivateSession} represents the bot's state for each registered
 * private session.
 */
@Data
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrivateSession {

    /** The API key. */
    private String apiKey;

    /** The guild id. */
    @Id
    private String authorId;

    /** The client ID. */
    private int clientId;

    /** When the session was added. */
    private Instant dateRegistered;

    /** The last notification id. */
    private int lastNotificationId;
}
