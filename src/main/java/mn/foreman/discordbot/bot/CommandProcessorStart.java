package mn.foreman.discordbot.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;

/** First interaction with the bot to kick-off registration. */
public class CommandProcessorStart
        implements CommandProcessor {

    /** The command prefix. */
    private final String commandPrefix;

    /** The dashboard URL. */
    private final String foremanDashboardUrl;

    /**
     * Constructor.
     *
     * @param commandPrefix       The command prefix.
     * @param foremanDashboardUrl The dashboard URL.
     */
    public CommandProcessorStart(
            final String commandPrefix,
            final String foremanDashboardUrl) {
        this.commandPrefix = commandPrefix;
        this.foremanDashboardUrl = foremanDashboardUrl;
    }

    @Override
    public void process(final MessageReceivedEvent event) {
        final MessageChannel messageChannel = event.getChannel();
        messageChannel
                .sendMessage(
                        new EmbedBuilder()
                                .setColor(Color.ORANGE)
                                .appendDescription("Hello! I'm **Todd**, the Foreman Discord notification bot. :wave:\n")
                                .appendDescription("\n")
                                .appendDescription(
                                        String.format(
                                                "Based on [triggers](%s/dashboard/triggers/) you create on your dashboard, I'll send you notifications when things happen.\n",
                                                this.foremanDashboardUrl))
                                .appendDescription("\n")
                                .appendDescription("Let's get introduced:\n")
                                .appendDescription("\n")
                                .appendDescription(
                                        String.format(
                                                "1. Go [here](%s/dashboard/profile/) get your **client id** and **API key**\n",
                                                this.foremanDashboardUrl))
                                .appendDescription(
                                        String.format(
                                                "2. Once you have them, run: `%s <client_id> <api_key>`\n",
                                                Command.REGISTER.getKey(this.commandPrefix)))
                                .appendDescription("3. That's it! :beers: Then I'll send your notifications to this channel.\n")
                                .appendDescription("\n")
                                .appendDescription("If you want them to happen somewhere else, re-run the register above in the channel where you want to be notified.")
                                .build())
                .queue();
    }
}
