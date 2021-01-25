package mn.foreman.discordbot.bot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link ListenerAdapter} that parses messages and dispatches them to the
 * {@link CommandProcessor processors}.
 */
public class MessageListener
        extends ListenerAdapter {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(MessageListener.class);

    /** The command prefix. */
    private final String commandPrefix;

    /** The processors. */
    private final Map<Command, CommandProcessor> commandProcessors;

    /**
     * Constructor.
     *
     * @param commandPrefix     The command prefix.
     * @param commandProcessors The processors.
     */
    public MessageListener(
            final String commandPrefix,
            final Map<Command, CommandProcessor> commandProcessors) {
        this.commandPrefix = commandPrefix;
        this.commandProcessors = new HashMap<>(commandProcessors);
    }

    @Override
    public void onMessageReceived(final @NotNull MessageReceivedEvent event) {
        final User author = event.getAuthor();
        if (!author.isBot()) {
            final Member member = event.getMember();
            if (isPermitted(member)) {
                final Message message = event.getMessage();
                final String text = message.getContentRaw();
                final Optional<Command> commandOptional =
                        Command.forText(
                                this.commandPrefix,
                                text);
                if (commandOptional.isPresent()) {
                    final Command command = commandOptional.get();
                    LOG.info("Running command {} from {}",
                            command,
                            event);
                    this.commandProcessors.getOrDefault(
                            command,
                            new CommandProcessorNull())
                            .process(event);
                } else {
                    LOG.info("Received a non-command: {}", message);
                }
            } else {
                LOG.info("Received message from non-permitted user: {}", author);
            }
        } else {
            LOG.info("Dropping message from bot {}", author);
        }
    }

    /**
     * Verifies that the provided member is allowed to manage this server.
     *
     * @param member The member.
     *
     * @return Whether or not the member is permitted.
     */
    private static boolean isPermitted(final Member member) {
        return (member != null &&
                member.hasPermission(Permission.MANAGE_SERVER));
    }
}
