package mn.foreman.discordbot.bot;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.awt.*;
import java.util.Optional;
import java.util.function.Function;

/** Stops the bot from notifying you. */
public class CommandProcessorForget<T>
        implements CommandProcessor {

    /** The ID supplier. */
    private final Function<MessageReceivedEvent, String> idSupplier;

    /** The session repository. */
    private final MongoRepository<T, String> sessionRepository;

    /**
     * Constructor.
     *
     * @param sessionRepository The session repository.
     * @param idSupplier        The ID supplier.
     */
    public CommandProcessorForget(
            final MongoRepository<T, String> sessionRepository,
            final Function<MessageReceivedEvent, String> idSupplier) {
        this.sessionRepository = sessionRepository;
        this.idSupplier = idSupplier;
    }

    @Override
    public void process(final MessageReceivedEvent event) {
        final String id = this.idSupplier.apply(event);
        final MessageChannel messageChannel = event.getChannel();

        final Optional<T> session = this.sessionRepository.findById(id);
        if (session.isPresent()) {
            this.sessionRepository.delete(session.get());
            MessageUtils.sendSimple(
                    "Got it - I won't send you notifications anymore",
                    Color.GREEN,
                    messageChannel);
        } else {
            MessageUtils.sendSimple(
                    "I don't think we've met...",
                    Color.RED,
                    messageChannel);
        }
    }
}
