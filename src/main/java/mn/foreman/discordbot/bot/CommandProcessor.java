package mn.foreman.discordbot.bot;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * A {@link CommandProcessor} provides a mechanism for processing received
 * Discord commands.
 */
public interface CommandProcessor {

    /**
     * Processes the provided event.
     *
     * @param event The event to process.
     */
    void process(MessageReceivedEvent event);
}
