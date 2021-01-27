package mn.foreman.discordbot.bot;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * An {@link EventRouter} provides a {@link CommandProcessor} implementation
 * that will route the event to the appropriate processor depending on where it
 * was received.
 */
public class EventRouter
        implements CommandProcessor {

    /** The guild processor. */
    private final CommandProcessor guildProcessor;

    /** The DM processor. */
    private final CommandProcessor privateMessageProcessor;

    /**
     * Constructor.
     *
     * @param guildProcessor          The guild processor.
     * @param privateMessageProcessor The DM processor.
     */
    public EventRouter(
            final CommandProcessor guildProcessor,
            final CommandProcessor privateMessageProcessor) {
        this.guildProcessor = guildProcessor;
        this.privateMessageProcessor = privateMessageProcessor;
    }

    @Override
    public void process(final MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            this.privateMessageProcessor.process(event);
        } else {
            this.guildProcessor.process(event);
        }
    }
}
