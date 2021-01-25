package mn.foreman.discordbot.bot;

import mn.foreman.api.ForemanApi;
import mn.foreman.api.ForemanApiImpl;
import mn.foreman.api.JdkWebUtil;
import mn.foreman.api.endpoints.notifications.Notifications;
import mn.foreman.discordbot.db.ChatSession;
import mn.foreman.discordbot.db.SessionRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import lombok.Builder;
import lombok.Data;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link NotificationsProcessor} implementation that sends
 * markdown-formatted messages to the provided chat based on the session that's
 * to be notified.
 */
@Component
public class NotificationsProcessorImpl
        implements NotificationsProcessor {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(NotificationsProcessorImpl.class);

    /** The API URL. */
    private final String foremanApiUrl;

    /** The base URL for Foreman. */
    private final String foremanDashboardUrl;

    /** The Discord API. */
    private final JDA jda;

    /** The max notifications to send at once. */
    private final int maxNotifications;

    /** The mapper. */
    private final ObjectMapper objectMapper;

    /** The session repository. */
    private final SessionRepository sessionRepository;

    /** The bot start time. */
    private final Instant startTime;

    /**
     * Constructor.
     *
     * @param sessionRepository   The session repository.
     * @param jda                 The Discord API.
     * @param objectMapper        The mapper.
     * @param startTime           The start time.
     * @param maxNotifications    The max notifications to send at once.
     * @param foremanApiUrl       The API URL.
     * @param foremanDashboardUrl The Foreman dashboard URL.
     */
    public NotificationsProcessorImpl(
            final SessionRepository sessionRepository,
            final JDA jda,
            final ObjectMapper objectMapper,
            final Instant startTime,
            @Value("${notifications.max}") final int maxNotifications,
            @Value("${foreman.apiUrl}") final String foremanApiUrl,
            @Value("${foreman.dashboardUrl}") final String foremanDashboardUrl) {
        this.sessionRepository = sessionRepository;
        this.jda = jda;
        this.objectMapper = objectMapper;
        this.startTime = startTime;
        this.maxNotifications = maxNotifications;
        this.foremanApiUrl = foremanApiUrl;
        this.foremanDashboardUrl = foremanDashboardUrl;
    }

    @Override
    public void process(
            final ChatSession chatSession) {
        final ForemanApi foremanApi =
                new ForemanApiImpl(
                        Integer.toString(chatSession.getClientId()),
                        "",
                        this.objectMapper,
                        new JdkWebUtil(
                                this.foremanApiUrl,
                                chatSession.getApiKey()));

        final Notifications notificationsApi =
                foremanApi.notifications();

        final Instant registered = chatSession.getDateRegistered();

        final List<Notifications.Notification> notifications =
                notificationsApi.discord(
                        chatSession.getLastNotificationId(),
                        registered.isAfter(this.startTime)
                                ? registered
                                : this.startTime);

        LOG.info("Session {} has {} pending notifications",
                chatSession,
                notifications);
        if (!notifications.isEmpty()) {
            notifications
                    .stream()
                    .map(this::toNotificationMessage)
                    .forEach(message ->
                            MessageUtils.sendSimple(
                                    message.getMessage(),
                                    message.isError()
                                            ? Color.RED
                                            : Color.GREEN,
                                    Objects.requireNonNull(jda.getTextChannelById(chatSession.getChannelId()))));

            final Notifications.Notification lastNotification =
                    Iterables.getLast(notifications);
            chatSession.setLastNotificationId(lastNotification.id);
            this.sessionRepository.save(chatSession);
        }
    }

    /**
     * Appends the provided {@link Notifications.Notification.FailingMiner} as a
     * markdown list item.
     *
     * @param failingMiner  The miner.
     * @param stringBuilder The builder for creating the aggregated message.
     */
    private void appendMiner(
            final Notifications.Notification.FailingMiner failingMiner,
            final StringBuilder stringBuilder) {
        stringBuilder
                .append(
                        String.format(
                                "[%s](%s/dashboard/miners/%d/details/)",
                                failingMiner.miner,
                                this.foremanDashboardUrl,
                                failingMiner.minerId))
                .append("\n");
        failingMiner
                .diagnosis
                .forEach(
                        diag ->
                                stringBuilder
                                        .append(diag)
                                        .append("\n"));
        stringBuilder
                .append("\n");
    }

    /**
     * Converts the provided notification to a Discord message to be sent.
     *
     * @param notification The notification to process.
     *
     * @return The Discord, markdown-formatted message.
     */
    private DiscordNotification toNotificationMessage(
            final Notifications.Notification notification) {
        final StringBuilder messageBuilder =
                new StringBuilder();

        // Write the subject
        messageBuilder.append(
                String.format(
                        "**%s**",
                        notification.subject));

        final List<Notifications.Notification.FailingMiner> failingMiners =
                notification.failingMiners;
        if (!failingMiners.isEmpty()) {
            // Write the miners out as lists
            messageBuilder.append("\n\n");
            failingMiners
                    .stream()
                    .limit(this.maxNotifications)
                    .forEach(
                            miner ->
                                    appendMiner(
                                            miner,
                                            messageBuilder));
            if (failingMiners.size() > this.maxNotifications) {
                // Too many were failing
                messageBuilder
                        .append("\n\n")
                        .append(
                                String.format(
                                        "*...and %d more*",
                                        failingMiners.size() - this.maxNotifications))
                        .append("\n\n")
                        .append(
                                String.format(
                                        "Head to [your dashboard](%s/dashboard/) to see the rest",
                                        this.foremanDashboardUrl));
            }
        }

        return DiscordNotification
                .builder()
                .error(!failingMiners.isEmpty())
                .message(messageBuilder.toString())
                .build();
    }

    /** A wrapper around the message to send and whether or not it's an error. */
    @Data
    @Builder
    private static class DiscordNotification {

        /** Whether or not the notification represents an error. */
        private final boolean error;

        /** The message. */
        private final String message;
    }
}
