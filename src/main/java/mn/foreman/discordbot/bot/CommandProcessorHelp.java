package mn.foreman.discordbot.bot;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.Arrays;

/** Displays a help message. */
public class CommandProcessorHelp<T>
        implements CommandProcessor {

    /** The command prefix. */
    private final String commandPrefix;

    /**
     * Constructor.
     *
     * @param commandPrefix The command prefix.
     */
    public CommandProcessorHelp(final String commandPrefix) {
        this.commandPrefix = commandPrefix;
    }

    @Override
    public void process(final MessageReceivedEvent event) {
        final StringBuilder builder =
                new StringBuilder()
                        .append("Sure...here's what I can do for ya:\n\n");
        Arrays.stream(Command.values())
                .forEach(command ->
                        builder
                                .append("**")
                                .append(command.getKey(this.commandPrefix))
                                .append("**")
                                .append("\n")
                                .append(command.getDescription())
                                .append("\n\n"));
        MessageUtils.sendSimple(
                builder.toString(),
                Color.ORANGE,
                event.getChannel());
    }
}
