package mn.foreman.discordbot.bot;

import mn.foreman.api.ForemanApi;
import mn.foreman.api.ForemanApiImpl;
import mn.foreman.api.JdkWebUtil;
import mn.foreman.discordbot.db.ChatSession;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Utilities for interacting with the Foreman API. */
public class ForemanUtils {

    /**
     * Returns a new {@link ForemanApi} handler.
     *
     * @param clientId       The client ID.
     * @param apiKey         The client API key.
     * @param foremanBaseUrl The Foreman base URL.
     *
     * @return The new API handler.
     */
    public static ForemanApi toApi(
            final int clientId,
            final String apiKey,
            final String foremanBaseUrl) {
        return new ForemanApiImpl(
                Integer.toString(clientId),
                "",
                new ObjectMapper(),
                new JdkWebUtil(
                        foremanBaseUrl,
                        apiKey));
    }

    /**
     * Returns a new {@link ForemanApi} handler.
     *
     * @param chatSession    The session.
     * @param foremanBaseUrl The Foreman base URL.
     *
     * @return The new API handler.
     */
    public static ForemanApi toApi(
            final ChatSession chatSession,
            final String foremanBaseUrl) {
        return toApi(
                chatSession.getClientId(),
                chatSession.getApiKey(),
                foremanBaseUrl);
    }
}
