package mn.foreman.discordbot.bot;

import java.time.Instant;

/**
 * A {@link NotificationsProcessor} provides a mechanism for obtaining pending
 * Discord notifications for a chat via the Foreman API and sends notifications
 * to a chat accordingly.
 */
public interface NotificationsProcessor<T> {

    /**
     * Obtains notifications for the provided session and notifies the chat, as
     * necessary.
     *
     * @param session The session.
     */
    void process(
            int id,
            String apiKey,
            Instant dateRegistered,
            int lastNotificationId,
            T session);
}
