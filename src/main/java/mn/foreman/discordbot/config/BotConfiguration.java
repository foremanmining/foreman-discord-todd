package mn.foreman.discordbot.config;

import mn.foreman.discordbot.bot.*;
import mn.foreman.discordbot.db.SessionRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.security.auth.login.LoginException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Bot bean configuration. */
@Configuration
public class BotConfiguration {

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
            final SessionRepository sessionRepository) {
        final CommandProcessor startProcessor =
                new CommandProcessorStart(
                        commandPrefix,
                        foremanDashboardUrl);
        return new ImmutableMap.Builder<Command, CommandProcessor>()
                .put(
                        Command.START,
                        startProcessor)
                .put(
                        Command.REGISTER,
                        new CommandProcessorRegister(
                                sessionRepository,
                                foremanApiUrl,
                                foremanDashboardUrl))
                .put(
                        Command.TEST,
                        new CommandProcessorTest(
                                sessionRepository,
                                startProcessor,
                                foremanApiUrl))
                .build();
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