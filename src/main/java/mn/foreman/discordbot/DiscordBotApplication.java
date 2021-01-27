package mn.foreman.discordbot;

import mn.foreman.discordbot.db.SessionRepository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/** The Foreman Discord bot. */
@SpringBootApplication
@EnableMongoRepositories(basePackageClasses = SessionRepository.class)
public class DiscordBotApplication {

    /**
     * Application entry point.
     *
     * @param args The command line arguments.
     */
    public static void main(final String[] args) {
        SpringApplication.run(
                DiscordBotApplication.class,
                args);
    }
}
