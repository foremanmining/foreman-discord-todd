package mn.foreman.discordbot.db;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.Instant;

/** A {@link ChatSession} represents the bot's state for each registered guild. */
@Data
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    /** The API key. */
    private String apiKey;

    /** The notification channel. */
    private String channelId;

    /** The client ID. */
    private int clientId;

    /** When the session was added. */
    private Instant dateRegistered;

    /** The guild id. */
    @Id
    private String guildId;

    /** The last notification id. */
    private int lastNotificationId;
}
