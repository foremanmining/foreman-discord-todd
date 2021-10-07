package mn.foreman.discordbot.bot;

import mn.foreman.api.ForemanApi;
import mn.foreman.api.endpoints.ping.Ping;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Registers the bot for a client and API key. */
public class CommandProcessorRegister<T>
        implements CommandProcessor {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(CommandProcessorRegister.class);

    /** Applies the client ID and api key to the session. */
    private final ClientIdApplier<T> clientIdApplier;

    /** The Foreman API URL. */
    private final String foremanApiUrl;

    /** The dashboard URL. */
    private final String foremanDashboardUrl;

    /** Obtains the ID from the event. */
    private final Function<MessageReceivedEvent, String> idSupplier;

    /** Supplier for creating a new session. */
    private final Function<MessageReceivedEvent, T> newCallback;

    /** The repository for sessions. */
    private final MongoRepository<T, String> sessionRepository;

    /** Callback for updating an existing session. */
    private final BiFunction<T, MessageReceivedEvent, T> updateCallback;

    /**
     * Constructor.
     *
     * @param sessionRepository   The repository.
     * @param idSupplier          The ID supplier.
     * @param updateCallback      The callback for updating sessions.
     * @param newCallback         The supplier for new sessions.
     * @param clientIdApplier     The applier for assigning client ID and api
     *                            keys.
     * @param foremanApiUrl       The Foreman API base URL.
     * @param foremanDashboardUrl The dashboard URL.
     */
    public CommandProcessorRegister(
            final MongoRepository<T, String> sessionRepository,
            final Function<MessageReceivedEvent, String> idSupplier,
            final BiFunction<T, MessageReceivedEvent, T> updateCallback,
            final Function<MessageReceivedEvent, T> newCallback,
            final ClientIdApplier<T> clientIdApplier,
            final String foremanApiUrl,
            final String foremanDashboardUrl) {
        this.sessionRepository = sessionRepository;
        this.idSupplier = idSupplier;
        this.updateCallback = updateCallback;
        this.newCallback = newCallback;
        this.clientIdApplier = clientIdApplier;
        this.foremanApiUrl = foremanApiUrl;
        this.foremanDashboardUrl = foremanDashboardUrl;
    }

    @Override
    public void process(final MessageReceivedEvent event) {
        final String id = this.idSupplier.apply(event);
        final MessageChannel messageChannel = event.getChannel();
        final Message message = event.getMessage();

        final T session =
                this.sessionRepository.findById(id)
                        .map(t -> this.updateCallback.apply(t, event))
                        .orElseGet(() -> this.newCallback.apply(event));

        final String[] split =
                message
                        .getContentRaw()
                        // Defensive for user input error
                        .replace("<", "")
                        .replace(">", "")
                        .split(" ");
        if (split.length >= 3) {
            try {
                final int clientId = Integer.parseInt(split[1]);
                final String apiKey = split[2];

                final ForemanApi foremanApi =
                        ForemanUtils.toApi(
                                clientId,
                                apiKey,
                                this.foremanApiUrl);
                final Ping ping = foremanApi.ping();
                if (ping.pingClient()) {
                    handleSuccess(
                            clientId,
                            apiKey,
                            session,
                            messageChannel);
                } else {
                    MessageUtils.sendSimple(
                            "I tried those, but they didn't work",
                            Color.RED,
                            messageChannel);
                }
            } catch (final NumberFormatException nfe) {
                LOG.warn("Number not provided", nfe);
                MessageUtils.sendSimple(
                        "Client ID should have been a number",
                        Color.RED,
                        messageChannel);
            }
        } else {
            MessageUtils.sendError(messageChannel);
        }
    }

    /**
     * Bot successfully configured.
     *
     * @param clientId       The client ID.
     * @param apiKey         The API key.
     * @param chatSession    The session.
     * @param messageChannel The message channel.
     */
    private void handleSuccess(
            final int clientId,
            final String apiKey,
            final T chatSession,
            final MessageChannel messageChannel) {
        this.clientIdApplier.apply(
                chatSession,
                clientId,
                apiKey);

        MessageUtils.sendSimple(
                "Those look correct! Setup complete! :white_check_mark:\n" +
                        "\n" +
                        String.format(
                                "You'll get notified based on your *alert* [triggers](%s/dashboard/triggers/), so make sure you created some and set their _destination_ to Discord.\n",
                                this.foremanDashboardUrl) +
                        "\n" +
                        "If you've already done this, you should be good to go! :thumbsup:",
                Color.GREEN,
                messageChannel);
    }

    /** Applies the client ID and api key to the session. */
    @FunctionalInterface
    public interface ClientIdApplier<T> {

        /**
         * Applies the client ID and api key to the session.
         *
         * @param session  The session.
         * @param clientId The ID.
         * @param apiKey   The api key.
         */
        void apply(
                T session,
                int clientId,
                String apiKey);
    }
}
