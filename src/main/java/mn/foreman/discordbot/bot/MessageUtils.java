package mn.foreman.discordbot.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.awt.*;

/** Utilities for sending messages. */
public class MessageUtils {

    /**
     * Utility method to send a standard error message.
     *
     * @param messageChannel The destination.
     */
    public static void sendError(final MessageChannel messageChannel) {
        sendSimple(
                "Something doesn't seem right...",
                Color.RED,
                messageChannel);
    }

    /**
     * Utility method to send a message.
     *
     * @param message        The message to send.
     * @param color          The color.
     * @param messageChannel The destination.
     */
    public static void sendSimple(
            final String message,
            final Color color,
            final MessageChannel messageChannel) {
        messageChannel
                .sendMessage(
                        new EmbedBuilder()
                                .setColor(color)
                                .setDescription(message)
                                .build())
                .queue();
    }

    /**
     * Utility method to send a message.
     *
     * @param message        The message to send.
     * @param messageChannel The destination.
     */
    public static void sendSimple(
            final String message,
            final MessageChannel messageChannel) {
        sendSimple(
                message,
                Color.ORANGE,
                messageChannel);
    }
}
