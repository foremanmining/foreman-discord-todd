package mn.foreman.discordbot.bot;

import mn.foreman.api.ForemanApi;
import mn.foreman.api.endpoints.ping.Ping;
import mn.foreman.discordbot.db.ChatSession;
import mn.foreman.discordbot.db.SessionRepository;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;

/** Registers the bot for a client and API key. */
public class CommandProcessorRegister
        implements CommandProcessor {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(CommandProcessorRegister.class);

    /** The Foreman API URL. */
    private final String foremanApiUrl;

    /** The dashboard URL. */
    private final String foremanDashboardUrl;

    /** The repository for sessions. */
    private final SessionRepository sessionRepository;

    /**
     * Constructor.
     *
     * @param sessionRepository   The repository.
     * @param foremanApiUrl       The Foreman API base URL.
     * @param foremanDashboardUrl The dashboard URL.
     */
    public CommandProcessorRegister(
            final SessionRepository sessionRepository,
            final String foremanApiUrl,
            final String foremanDashboardUrl) {
        this.sessionRepository = sessionRepository;
        this.foremanApiUrl = foremanApiUrl;
        this.foremanDashboardUrl = foremanDashboardUrl;
    }

    @Override
    public void process(final MessageReceivedEvent event) {
        final String guildId = event.getGuild().getId();
        final MessageChannel messageChannel = event.getChannel();
        final String messageChannelId = messageChannel.getId();
        final Message message = event.getMessage();

        final ChatSession chatSession =
                this.sessionRepository.findById(guildId)
                        .map(session -> updateSession(session, messageChannelId))
                        .orElseGet(() -> newSession(messageChannelId, guildId));

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
                            chatSession,
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
            final ChatSession chatSession,
            final MessageChannel messageChannel) {
        chatSession.setClientId(clientId);
        chatSession.setApiKey(apiKey);
        chatSession.setDateRegistered(Instant.now());
        this.sessionRepository.save(chatSession);

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

    /**
     * First time this guild has been seen - new session.
     *
     * @param channelId The channel.
     * @param guildId   The guild.
     *
     * @return The new session.
     */
    private ChatSession newSession(
            final String channelId,
            final String guildId) {
        return this.sessionRepository.insert(
                ChatSession
                        .builder()
                        .guildId(guildId)
                        .channelId(channelId)
                        .build());
    }

    /**
     * Register was ran in a new channel. Update the session so notifications go
     * here instead.
     *
     * @param session   The session.
     * @param channelId The new channel.
     *
     * @return The session.
     */
    private ChatSession updateSession(
            final ChatSession session,
            final String channelId) {
        session.setChannelId(channelId);
        this.sessionRepository.save(session);
        return session;
    }
}
