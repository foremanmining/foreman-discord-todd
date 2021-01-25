package mn.foreman.discordbot.bot;

import mn.foreman.discordbot.db.ChatSession;

/**
 * A {@link NotificationsProcessor} provides a mechanism for obtaining pending
 * Discord notifications for a chat via the Foreman API and sends notifications
 * to a chat accordingly.
 */
public interface NotificationsProcessor {

    /**
     * Obtains notifications for the provided session and notifies the chat, as
     * necessary.
     *
     * @param chatSession The session.
     */
    void process(ChatSession chatSession);
}
