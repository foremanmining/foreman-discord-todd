package mn.foreman.discordbot.bot;

import mn.foreman.api.ForemanApi;
import mn.foreman.api.ForemanApiImpl;
import mn.foreman.api.JdkWebUtil;
import mn.foreman.api.endpoints.notifications.Notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * A simple {@link NotificationsProcessor} implementation that sends
 * markdown-formatted messages to the provided chat based on the session that's
 * to be notified.
 */
public class NotificationsProcessorImpl<T>
        implements NotificationsProcessor<T> {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(NotificationsProcessorImpl.class);

    /** The API URL. */
    private final String foremanApiUrl;

    /** The base URL for Foreman. */
    private final String foremanDashboardUrl;

    /** The setter for the last notification ID. */
    private final BiConsumer<T, Integer> lastNotificationSetter;

    /** The max notifications to send at once. */
    private final int maxNotifications;

    /** The mapper. */
    private final ObjectMapper objectMapper;

    /** The message channel supplier. */
    private final BiConsumer<DiscordNotification, T> sender;

    /** The session repository. */
    private final MongoRepository<T, String> sessionRepository;

    /** The bot start time. */
    private final Instant startTime;

    /**
     * Constructor.
     *
     * @param sessionRepository      The session repository.
     * @param sender                 The sender callback.
     * @param lastNotificationSetter The last notification ID setter.
     * @param objectMapper           The mapper.
     * @param startTime              The start time.
     * @param maxNotifications       The max notifications to send at once.
     * @param foremanApiUrl          The API URL.
     * @param foremanDashboardUrl    The Foreman dashboard URL.
     */
    public NotificationsProcessorImpl(
            final MongoRepository<T, String> sessionRepository,
            final BiConsumer<DiscordNotification, T> sender,
            final BiConsumer<T, Integer> lastNotificationSetter,
            final ObjectMapper objectMapper,
            final Instant startTime,
            final int maxNotifications,
            final String foremanApiUrl,
            final String foremanDashboardUrl) {
        this.sessionRepository = sessionRepository;
        this.sender = sender;
        this.lastNotificationSetter = lastNotificationSetter;
        this.objectMapper = objectMapper;
        this.startTime = startTime;
        this.maxNotifications = maxNotifications;
        this.foremanApiUrl = foremanApiUrl;
        this.foremanDashboardUrl = foremanDashboardUrl;
    }

    @Override
    public void process(
            final int id,
            final String apiKey,
            final Instant dateRegistered,
            final int lastNotificationId,
            final T session) {
        final ForemanApi foremanApi =
                new ForemanApiImpl(
                        Integer.toString(id),
                        "",
                        this.objectMapper,
                        new JdkWebUtil(
                                this.foremanApiUrl,
                                apiKey,
                                5,
                                TimeUnit.SECONDS));

        final Notifications notificationsApi =
                foremanApi.notifications();

        final List<Notifications.Notification> notifications =
                notificationsApi.discord(
                        lastNotificationId,
                        dateRegistered.isAfter(this.startTime)
                                ? dateRegistered
                                : this.startTime);

        LOG.info("Session {} has {} pending notifications",
                session,
                notifications);
        if (!notifications.isEmpty()) {
            LOG.debug("Building notification message for {}", session);
            notifications
                    .stream()
                    .map(this::toNotificationMessage)
                    .forEach(message ->
                            this.sender.accept(
                                    message,
                                    session));

            final Notifications.Notification lastNotification =
                    Iterables.getLast(notifications);
            this.lastNotificationSetter.accept(session, lastNotification.id);
            this.sessionRepository.save(session);
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
    public static class DiscordNotification {

        /** Whether or not the notification represents an error. */
        private final boolean error;

        /** The message. */
        private final String message;
    }
}
