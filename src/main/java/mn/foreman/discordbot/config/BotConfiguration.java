package mn.foreman.discordbot.config;

import mn.foreman.discordbot.bot.*;
import mn.foreman.discordbot.db.ChatSession;
import mn.foreman.discordbot.db.PrivateSession;
import mn.foreman.discordbot.db.PrivateSessionRepository;
import mn.foreman.discordbot.db.SessionRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Bot bean configuration. */
@Configuration
public class BotConfiguration {

    /** The notifier fixed deplay. */
    @Value("${bot.check.fixedDelay}")
    private long fixedDelay;

    /** The notifier initial delay. */
    @Value("${bot.check.initialDelay}")
    private long initialDelay;

    /** The notifiers. */
    @Autowired
    private List<Notifier<?>> notifiers;

    /** The thread pool. */
    @Autowired
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * Creates the notifier for managing guild sessions.
     *
     * @param sessionRepository   The session repository.
     * @param jda                 The JDA.
     * @param objectMapper        The mapper for JSON.
     * @param startTime           When the application started.
     * @param maxNotifications    The maximum number of notifications to send.
     * @param foremanApiUrl       The API URL.
     * @param foremanDashboardUrl The dashboard URL.
     *
     * @return The notifier.
     */
    @Bean
    public Notifier<ChatSession> chatSessionNotifier(
            final SessionRepository sessionRepository,
            final JDA jda,
            final ObjectMapper objectMapper,
            final Instant startTime,
            @Value("${notifications.max}") final int maxNotifications,
            @Value("${foreman.apiUrl}") final String foremanApiUrl,
            @Value("${foreman.dashboardUrl}") final String foremanDashboardUrl) {
        final NotificationsProcessor<ChatSession> notificationsProcessor =
                new NotificationsProcessorImpl<>(
                        sessionRepository,
                        chatSession -> jda.getTextChannelById(chatSession.getChannelId()),
                        ChatSession::setLastNotificationId,
                        objectMapper,
                        startTime,
                        maxNotifications,
                        foremanApiUrl,
                        foremanDashboardUrl);
        return new Notifier<>(
                sessionRepository,
                chatSession ->
                        chatSession.getDateRegistered() != null,
                chatSession ->
                        notificationsProcessor.process(
                                chatSession.getClientId(),
                                chatSession.getApiKey(),
                                chatSession.getDateRegistered(),
                                chatSession.getLastNotificationId(),
                                chatSession));
    }

    /**
     * Creates the command processors.
     *
     * @param commandPrefix       The command prefix.
     * @param foremanApiUrl       The Foreman API URL.
     * @param foremanDashboardUrl The Foreman dashboard URL.
     * @param sessionRepository   The session repository.
     *
     * @return The processors.
     */
    @Bean
    public Map<Command, CommandProcessor> commandProcessors(
            @Value("${bot.commandPrefix}") final String commandPrefix,
            @Value("${foreman.apiUrl}") final String foremanApiUrl,
            @Value("${foreman.dashboardUrl}") final String foremanDashboardUrl,
            final SessionRepository sessionRepository,
            final PrivateSessionRepository privateSessionRepository) {
        final CommandProcessor startProcessor =
                new CommandProcessorStart(
                        commandPrefix,
                        foremanDashboardUrl);
        return new ImmutableMap.Builder<Command, CommandProcessor>()
                .put(
                        Command.START,
                        startProcessor)
                .put(
                        Command.HELP,
                        new CommandProcessorHelp<>(commandPrefix))
                .put(
                        Command.FORGET,
                        new EventRouter(
                                new CommandProcessorForget<>(
                                        sessionRepository,
                                        event -> event.getGuild().getId()),
                                new CommandProcessorForget<>(
                                        privateSessionRepository,
                                        event -> event.getAuthor().getId())))
                .put(
                        Command.REGISTER,
                        new EventRouter(
                                new CommandProcessorRegister<>(
                                        sessionRepository,
                                        event -> event.getGuild().getId(),
                                        (session, id) -> {
                                            session.setChannelId(id);
                                            sessionRepository.save(session);
                                            return session;
                                        },
                                        (channelId, id) ->
                                                sessionRepository.insert(
                                                        ChatSession
                                                                .builder()
                                                                .guildId(id)
                                                                .channelId(channelId)
                                                                .build()),
                                        (session, clientId, apiKey) -> {
                                            session.setClientId(clientId);
                                            session.setApiKey(apiKey);
                                            session.setDateRegistered(Instant.now());
                                            sessionRepository.save(session);
                                        },
                                        foremanApiUrl,
                                        foremanDashboardUrl),
                                new CommandProcessorRegister<>(
                                        privateSessionRepository,
                                        event -> event.getAuthor().getId(),
                                        (session, id) -> {
                                            session.setAuthorId(id);
                                            privateSessionRepository.save(session);
                                            return session;
                                        },
                                        (channelId, id) ->
                                                privateSessionRepository.insert(
                                                        PrivateSession
                                                                .builder()
                                                                .authorId(id)
                                                                .build()),
                                        (session, clientId, apiKey) -> {
                                            session.setClientId(clientId);
                                            session.setApiKey(apiKey);
                                            session.setDateRegistered(Instant.now());
                                            privateSessionRepository.save(session);
                                        },
                                        foremanApiUrl,
                                        foremanDashboardUrl)))
                .put(
                        Command.TEST,
                        new EventRouter(
                                new CommandProcessorTest<>(
                                        sessionRepository,
                                        event -> event.getGuild().getId(),
                                        session ->
                                                ForemanUtils.toApi(
                                                        session.getClientId(),
                                                        session.getApiKey(),
                                                        foremanApiUrl),
                                        startProcessor),
                                new CommandProcessorTest<>(
                                        privateSessionRepository,
                                        event -> event.getAuthor().getId(),
                                        session ->
                                                ForemanUtils.toApi(
                                                        session.getClientId(),
                                                        session.getApiKey(),
                                                        foremanApiUrl),
                                        startProcessor)))
                .build();
    }

    /**
     * Creates a new thread pool.
     *
     * @return The thread pool.
     */
    @Bean
    public ScheduledExecutorService executorService() {
        return Executors.newScheduledThreadPool(2);
    }

    /**
     * Creates the {@link JDA}.
     *
     * @param token            The token.
     * @param activity         The activity message.
     * @param messageListeners The listeners.
     *
     * @return The new {@link JDA}.
     *
     * @throws LoginException       on failure.
     * @throws InterruptedException on failure.
     */
    @Bean
    public JDA jda(
            @Value("${bot.token}") final String token,
            @Value("${bot.activity}") final String activity,
            final List<MessageListener> messageListeners)
            throws
            LoginException,
            InterruptedException {
        final JDA jda =
                JDABuilder.createDefault(token)
                        .setActivity(Activity.watching(activity))
                        .build()
                        .awaitReady();
        messageListeners.forEach(jda::addEventListener);
        return jda;
    }

    /**
     * Creates the listener for messages.
     *
     * @param commandPrefix     The command prefix.
     * @param commandProcessors The processors.
     *
     * @return The listener.
     */
    @Bean
    public MessageListener messageListener(
            @Value("${bot.commandPrefix}") final String commandPrefix,
            final Map<Command, CommandProcessor> commandProcessors) {
        return new MessageListener(
                commandPrefix,
                commandProcessors);
    }

    /**
     * Returns a new JSON {@link ObjectMapper}.
     *
     * @return The mapper.
     */
    @Bean
    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    /** Starts the notifiers. */
    @PostConstruct
    public void post() {
        for (final Notifier<?> notifier : notifiers) {
            scheduledExecutorService.scheduleAtFixedRate(
                    notifier::fetchAndNotify,
                    initialDelay,
                    fixedDelay,
                    TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Creates the notifier for managing private sessions.
     *
     * @param sessionRepository   The session repository.
     * @param jda                 The JDA.
     * @param objectMapper        The mapper for JSON.
     * @param startTime           When the application started.
     * @param maxNotifications    The maximum number of notifications to send.
     * @param foremanApiUrl       The API URL.
     * @param foremanDashboardUrl The dashboard URL.
     *
     * @return The notifier.
     */
    @Bean
    public Notifier<PrivateSession> privateSessionNotifier(
            final PrivateSessionRepository sessionRepository,
            final JDA jda,
            final ObjectMapper objectMapper,
            final Instant startTime,
            @Value("${notifications.max}") final int maxNotifications,
            @Value("${foreman.apiUrl}") final String foremanApiUrl,
            @Value("${foreman.dashboardUrl}") final String foremanDashboardUrl) {
        final NotificationsProcessor<PrivateSession> notificationsProcessor =
                new NotificationsProcessorImpl<>(
                        sessionRepository,
                        session -> {
                            final User user = jda.getUserById(session.getAuthorId());
                            if (user != null) {
                                return user.openPrivateChannel().complete();
                            }
                            return null;
                        },
                        PrivateSession::setLastNotificationId,
                        objectMapper,
                        startTime,
                        maxNotifications,
                        foremanApiUrl,
                        foremanDashboardUrl);
        return new Notifier<>(
                sessionRepository,
                chatSession ->
                        chatSession.getDateRegistered() != null,
                chatSession ->
                        notificationsProcessor.process(
                                chatSession.getClientId(),
                                chatSession.getApiKey(),
                                chatSession.getDateRegistered(),
                                chatSession.getLastNotificationId(),
                                chatSession));
    }

    /**
     * Returns the application start time.
     *
     * @return The application start time.
     */
    @Bean
    public Instant startTime() {
        return Instant.now();
    }
}