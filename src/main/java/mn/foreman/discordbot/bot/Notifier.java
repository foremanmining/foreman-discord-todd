package mn.foreman.discordbot.bot;

import mn.foreman.discordbot.db.ChatSession;
import mn.foreman.discordbot.db.SessionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Queries the Foreman API for notifications and sends them. */
@Component
public class Notifier {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(Notifier.class);

    /** The processor for notifications. */
    private final NotificationsProcessor notificationsProcessor;

    /** The repository. */
    private final SessionRepository sessionRepository;

    /**
     * Constructor.
     *
     * @param sessionRepository The repository.
     */
    public Notifier(
            final SessionRepository sessionRepository,
            final NotificationsProcessor notificationsProcessor) {
        this.sessionRepository = sessionRepository;
        this.notificationsProcessor = notificationsProcessor;
    }

    /**
     * Periodically fetches notifications for each session that this bot
     * monitors and sends messages to users, as necessary.
     */
    @Scheduled(
            initialDelayString = "${bot.check.initialDelay}",
            fixedDelayString = "${bot.check.fixedDelay}")
    public void fetchAndNotify() {
        final List<ChatSession> sessions =
                this.sessionRepository.findAll();
        LOG.info("Looking for notifications for {} sessions", sessions.size());
        sessions
                .parallelStream()
                .filter(session -> session.getDateRegistered() != null)
                .forEach(this.notificationsProcessor::process);
    }
}
