package mn.foreman.discordbot.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Queries the Foreman API for notifications and sends them. */
public class Notifier<T> {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(Notifier.class);

    /** The session filter. */
    private final Predicate<T> filter;

    /** The processor for notifications. */
    private final Consumer<T> notificationsProcessor;

    /** The repository. */
    private final MongoRepository<T, String> sessionRepository;

    /**
     * Constructor.
     *
     * @param sessionRepository      The repository.
     * @param filter                 The session filter.
     * @param notificationsProcessor The notification processor.
     */
    public Notifier(
            final MongoRepository<T, String> sessionRepository,
            final Predicate<T> filter,
            final Consumer<T> notificationsProcessor) {
        this.sessionRepository = sessionRepository;
        this.filter = filter;
        this.notificationsProcessor = notificationsProcessor;
    }

    /**
     * Periodically fetches notifications for each session that this bot
     * monitors and sends messages to users, as necessary.
     */
    public void fetchAndNotify() {
        try {
            final List<T> sessions =
                    this.sessionRepository.findAll();
            LOG.info("Looking for notifications for {} sessions", sessions.size());
            sessions
                    .parallelStream()
                    .filter(this.filter)
                    .forEach(this.notificationsProcessor);
        } catch (final Exception e) {
            LOG.warn("Exception occurred while processing notifications", e);
        }
    }
}
