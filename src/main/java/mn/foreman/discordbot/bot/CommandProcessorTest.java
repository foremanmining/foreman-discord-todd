package mn.foreman.discordbot.bot;

import mn.foreman.api.ForemanApi;
import mn.foreman.api.endpoints.ping.Ping;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.awt.*;
import java.util.Optional;
import java.util.function.Function;

/** Tests connectivity to the Foreman API. */
public class CommandProcessorTest<T>
        implements CommandProcessor {

    /** The supplier for API handlers. */
    private final Function<T, ForemanApi> apiSupplier;

    /** Obtains the ID from the event. */
    private final Function<MessageReceivedEvent, String> idSupplier;

    /** The session repository. */
    private final MongoRepository<T, String> sessionRepository;

    /** The start processor. */
    private final CommandProcessor startProcessor;

    /**
     * Constructor.
     *
     * @param sessionRepository The session repository.
     * @param idSupplier        The supplier for IDs.
     * @param apiSupplier       The supplier for new API handlers.
     * @param startProcessor    The start processor.
     */
    public CommandProcessorTest(
            final MongoRepository<T, String> sessionRepository,
            final Function<MessageReceivedEvent, String> idSupplier,
            final Function<T, ForemanApi> apiSupplier,
            final CommandProcessor startProcessor) {
        this.sessionRepository = sessionRepository;
        this.idSupplier = idSupplier;
        this.apiSupplier = apiSupplier;
        this.startProcessor = startProcessor;
    }

    @Override
    public void process(final MessageReceivedEvent event) {
        final MessageChannel messageChannel = event.getChannel();

        final String id = this.idSupplier.apply(event);

        final Optional<T> sessionOpt =
                this.sessionRepository.findById(id);
        if (sessionOpt.isPresent()) {
            final T session = sessionOpt.get();
            final ForemanApi foremanApi =
                    this.apiSupplier.apply(session);
            final Ping ping = foremanApi.ping();

            MessageUtils.sendSimple(
                    "Checking connectivity to Foreman...",
                    messageChannel);
            if (ping.ping()) {
                MessageUtils.sendSimple(
                        "*Result*: :white_check_mark:",
                        Color.GREEN,
                        messageChannel);
            } else {
                MessageUtils.sendSimple(
                        "*Result*: :x:",
                        Color.RED,
                        messageChannel);
            }

            MessageUtils.sendSimple(
                    "Checking authentication with your API credentials...",
                    messageChannel);
            if (ping.pingClient()) {
                MessageUtils.sendSimple(
                        "*Result*: :white_check_mark:",
                        Color.GREEN,
                        messageChannel);
            } else {
                MessageUtils.sendSimple(
                        "*Result*: :x:",
                        Color.RED,
                        messageChannel);
            }
        } else {
            MessageUtils.sendSimple(
                    "We haven't met yet...",
                    Color.RED,
                    messageChannel);
            this.startProcessor.process(event);
        }
    }
}
