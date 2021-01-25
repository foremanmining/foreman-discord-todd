package mn.foreman.discordbot.bot;

import mn.foreman.api.ForemanApi;
import mn.foreman.api.endpoints.ping.Ping;
import mn.foreman.discordbot.db.ChatSession;
import mn.foreman.discordbot.db.SessionRepository;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.Optional;

/** Tests connectivity to the Foreman API. */
public class CommandProcessorTest
        implements CommandProcessor {

    /** The Foreman base URL. */
    private final String foremanApiUrl;

    /** The session repository. */
    private final SessionRepository sessionRepository;

    /** The start processor. */
    private final CommandProcessor startProcessor;

    /**
     * Constructor.
     *
     * @param sessionRepository The session repository.
     * @param startProcessor    The start processor.
     * @param foremanApiUrl     The Foreman API base URL.
     */
    public CommandProcessorTest(
            final SessionRepository sessionRepository,
            final CommandProcessor startProcessor,
            final String foremanApiUrl) {
        this.sessionRepository = sessionRepository;
        this.startProcessor = startProcessor;
        this.foremanApiUrl = foremanApiUrl;
    }

    @Override
    public void process(final MessageReceivedEvent event) {
        final MessageChannel messageChannel = event.getChannel();
        final String guildId = event.getGuild().getId();

        final Optional<ChatSession> chatSessionOpt =
                this.sessionRepository.findById(guildId);
        if (chatSessionOpt.isPresent()) {
            final ChatSession chatSession = chatSessionOpt.get();
            final ForemanApi foremanApi =
                    ForemanUtils.toApi(
                            chatSession,
                            this.foremanApiUrl);
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
