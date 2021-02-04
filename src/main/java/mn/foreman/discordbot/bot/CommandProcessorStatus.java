package mn.foreman.discordbot.bot;

import mn.foreman.api.ForemanApi;
import mn.foreman.api.endpoints.miners.Miners;
import mn.foreman.api.endpoints.pickaxe.Pickaxe;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Gets the status of all of the miners in Foreman. */
public class CommandProcessorStatus<T>
        implements CommandProcessor {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(CommandProcessorStatus.class);

    /** The supplier for creating new API handlers. */
    private final BiFunction<T, String, ForemanApi> apiSupplier;

    /** The dashboard URL. */
    private final String foremanDashboardUrl;

    /** Obtains the ID from the event. */
    private final Function<MessageReceivedEvent, String> idSupplier;

    /** The repository for sessions. */
    private final MongoRepository<T, String> sessionRepository;

    /**
     * Constructor.
     *
     * @param sessionRepository   The repository.
     * @param idSupplier          The ID supplier.
     * @param apiSupplier         The API supplier.
     * @param foremanDashboardUrl The dashboard URL.
     */
    public CommandProcessorStatus(
            final MongoRepository<T, String> sessionRepository,
            final Function<MessageReceivedEvent, String> idSupplier,
            final BiFunction<T, String, ForemanApi> apiSupplier,
            final String foremanDashboardUrl) {
        this.sessionRepository = sessionRepository;
        this.idSupplier = idSupplier;
        this.apiSupplier = apiSupplier;
        this.foremanDashboardUrl = foremanDashboardUrl;
    }

    @Override
    public void process(final MessageReceivedEvent event) {
        final String id = this.idSupplier.apply(event);
        final MessageChannel messageChannel = event.getChannel();

        final Optional<T> sessionOpt = this.sessionRepository.findById(id);
        if (sessionOpt.isPresent()) {
            final T session = sessionOpt.get();
            final ForemanApi foremanApi =
                    this.apiSupplier.apply(
                            session,
                            "");

            final List<Pickaxe.PickaxeInstance> pickaxes =
                    foremanApi.pickaxe().all();
            final Map<String, List<Miners.Miner>> troubleMiners =
                    pickaxes
                            .stream()
                            .map(pickaxe -> getMiners(session, pickaxe))
                            .flatMap(List::stream)
                            .filter(miner -> !miner.status.equals("okay"))
                            .collect(Collectors.groupingBy(miner -> miner.status));
            if (!troubleMiners.isEmpty()) {
                final List<Miners.Miner> failingMiners =
                        troubleMiners.getOrDefault(
                                "fail",
                                Collections.emptyList());
                String discordMessage =
                        toMessage(failingMiners);
                discordMessage +=
                        toMessage(
                                troubleMiners.getOrDefault(
                                        "warn",
                                        Collections.emptyList()));

                MessageUtils.sendSimple(
                        discordMessage,
                        failingMiners.isEmpty()
                                ? Color.ORANGE
                                : Color.RED,
                        messageChannel);
            } else {
                MessageUtils.sendSimple(
                        "Everything looks okay!",
                        Color.GREEN,
                        messageChannel);
            }
        } else {
            MessageUtils.sendSimple(
                    "We haven't met yet...",
                    Color.RED,
                    messageChannel);
        }
    }

    /**
     * Obtains the miners on the provided pickaxe.
     *
     * @param session The session.
     * @param pickaxe The pickaxe.
     *
     * @return The miners.
     */
    private List<Miners.Miner> getMiners(
            final T session,
            final Pickaxe.PickaxeInstance pickaxe) {
        final ForemanApi foremanApi =
                this.apiSupplier.apply(
                        session,
                        pickaxe.key);
        return foremanApi.miners().all();
    }

    /**
     * Creates a string containing the message.
     *
     * @param failingMiners The failing miners.
     *
     * @return The message.
     */
    private String toMessage(final List<Miners.Miner> failingMiners) {
        return "";
    }

    /**
     * Creates a URL with the provided miner's info.
     *
     * @param miner The miner.
     *
     * @return The markdown link.
     */
    private String toMinerUrl(final Miners.Miner miner) {
        return String.format(
                "[%s](%s/dashboard/miners/%d/details/)",
                miner.name,
                this.foremanDashboardUrl,
                miner.id);
    }
}
