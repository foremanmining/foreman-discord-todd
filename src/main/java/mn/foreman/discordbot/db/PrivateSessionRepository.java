package mn.foreman.discordbot.db;

import org.springframework.data.mongodb.repository.MongoRepository;

/** A repository for storing {@link PrivateSession sessions}. */
public interface PrivateSessionRepository
        extends MongoRepository<PrivateSession, String> {
    
}
