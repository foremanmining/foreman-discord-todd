package mn.foreman.discordbot.config;

import mn.foreman.api.ForemanApiImpl;
import mn.foreman.api.JdkWebUtil;
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
import net.dv8tion.jda.api.entities.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;
import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Bot bean configuration. */
@Configuration
public class BotConfiguration {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(BotConfiguration.class);

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
                        (notification, session) -> {
                            final MessageChannel messageChannel =
                                    jda.getTextChannelById(session.getChannelId());
                            if (messageChannel != null) {
                                MessageUtils.sendSimple(
                                        notification.getMessage(),
                                        notification.isError()
                                                ? Color.RED
                                                : Color.GREEN,
                                        messageChannel);
                            } else {
                                LOG.warn("Failed to obtain channel for {}",
                                        session);
                                sessionRepository.delete(session);
                            }
                        },
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
     * @param objectMapper        The mapper.
     *
     * @return The processors.
     */
    @Bean
    public Map<Command, CommandProcessor> commandProcessors(
            @Value("${bot.commandPrefix}") final String commandPrefix,
            @Value("${foreman.apiUrl}") final String foremanApiUrl,
            @Value("${foreman.dashboardUrl}") final String foremanDashboardUrl,
            final SessionRepository sessionRepository,
            final PrivateSessionRepository privateSessionRepository,
            final ObjectMapper objectMapper) {
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
                                        (session, event) -> {
                                            session.setChannelId(event.getGuild().getId());
                                            sessionRepository.save(session);
                                            return session;
                                        },
                                        (event) ->
                                                sessionRepository.insert(
                                                        ChatSession
                                                                .builder()
                                                                .guildId(event.getGuild().getId())
                                                                .channelId(event.getChannel().getId())
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
                                        (session, event) -> {
                                            session.setAuthorId(event.getAuthor().getId());
                                            privateSessionRepository.save(session);
                                            return session;
                                        },
                                        (event) ->
                                                privateSessionRepository.insert(
                                                        PrivateSession
                                                                .builder()
                                                                .authorId(event.getAuthor().getId())
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
                .put(
                        Command.STATUS,
                        new EventRouter(
                                new CommandProcessorStatus<>(
                                        sessionRepository,
                                        event -> event.getGuild().getId(),
                                        (session, pickaxe) ->
                                                new ForemanApiImpl(
                                                        Integer.toString(session.getClientId()),
                                                        pickaxe,
                                                        objectMapper,
                                                        new JdkWebUtil(
                                                                foremanApiUrl,
                                                                session.getApiKey(),
                                                                5,
                                                                TimeUnit.SECONDS)),
                                        foremanDashboardUrl),
                                new CommandProcessorStatus<>(
                                        privateSessionRepository,
                                        event -> event.getAuthor().getId(),
                                        (session, pickaxe) ->
                                                new ForemanApiImpl(
                                                        Integer.toString(session.getClientId()),
                                                        pickaxe,
                                                        objectMapper,
                                                        new JdkWebUtil(
                                                                foremanApiUrl,
                                                                session.getApiKey(),
                                                                5,
                                                                TimeUnit.SECONDS)),
                                        foremanDashboardUrl)))
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
        for (final Notifier<?> notifier : this.notifiers) {
            this.scheduledExecutorService.scheduleAtFixedRate(
                    notifier::fetchAndNotify,
                    this.initialDelay,
                    this.fixedDelay,
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
                        (notification, session) ->
                                jda
                                        .retrieveUserById(session.getAuthorId())
                                        .queue(user -> {
                                            if (user != null) {
                                                user.openPrivateChannel().queue(
                                                        privateChannel ->
                                                                MessageUtils.sendSimple(
                                                                        notification.getMessage(),
                                                                        notification.isError()
                                                                                ? Color.RED
                                                                                : Color.GREEN,
                                                                        privateChannel));
                                            } else {
                                                LOG.warn("User doesn't exist for {}", session);
                                                sessionRepository.delete(session);
                                            }
                                        }),
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