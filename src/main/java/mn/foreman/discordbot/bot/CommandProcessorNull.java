package mn.foreman.discordbot.bot;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/** A do-nothing processor. */
public class CommandProcessorNull
        implements CommandProcessor {

    @Override
    public void process(final MessageReceivedEvent event) {
        // Do nothing
    }
}
