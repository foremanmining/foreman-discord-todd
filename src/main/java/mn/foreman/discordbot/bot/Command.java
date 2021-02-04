package mn.foreman.discordbot.bot;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** All of the known commands. */
public enum Command {

    /** Displays the help menu. */
    HELP(
            "help",
            "Displays a help menu"),

    /** Start bot setup. */
    START(
            "start",
            "Begins the bot setup process"),

    /** Begins the registration process. */
    REGISTER(
            "register",
            "Registers the bot with new API credentials. Notifications will be sent to the channel where the registration was performed."),

    /** Test bot connectivity. */
    TEST(
            "test",
            "Tests connectivity with the Foreman API"),

    /** Stops the bot from notifying you. */
    FORGET(
            "forget",
            "Stops the bot from notifying you"),

    /** The status command. */
    STATUS(
            "status",
            "Displays the current non-okay status for each miner in Foreman");

    /** All of the known commands. */
    private static final ConcurrentMap<String, Command> VALUES =
            new ConcurrentHashMap<>();

    static {
        for (final Command command : values()) {
            VALUES.put(command.key, command);
        }
    }

    /** The description. */
    private final String description;

    /** The key. */
    private final String key;

    /**
     * Constructor.
     *
     * @param key         The key.
     * @param description The description.
     */
    Command(
            final String key,
            final String description) {
        this.key = key;
        this.description = description;
    }

    /**
     * Returns the command related to the text.
     *
     * @param commandPrefix The command prefix.
     * @param text          The text.
     *
     * @return The command.
     */
    public static Optional<Command> forText(
            final String commandPrefix,
            final String text) {
        if (text.startsWith(commandPrefix)) {
            final String[] regions =
                    text
                            .replace(commandPrefix, "")
                            .split(" ");
            if (regions.length > 0) {
                return Optional.ofNullable(VALUES.get(regions[0]));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the description.
     *
     * @return The description.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the key.
     *
     * @param commandPrefix The prefix.
     *
     * @return The key.
     */
    public String getKey(final String commandPrefix) {
        return commandPrefix + this.key;
    }
}
