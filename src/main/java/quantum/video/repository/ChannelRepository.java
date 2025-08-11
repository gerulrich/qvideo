package quantum.video.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import quantum.video.model.Channel;

/**
 * Repository for managing Channel entities in MongoDB.
 * <p>
 * This repository provides reactive access to Channel documents stored in MongoDB,
 * leveraging Quarkus Panache for simplified data access patterns. It enables non-blocking
 * CRUD operations and custom queries on Channel entities.
 * <p>
 * The repository is application-scoped to ensure a single instance exists per application.
 *
 * @see Channel
 * @see ReactivePanacheMongoRepository
 */
@ApplicationScoped
public class ChannelRepository implements ReactivePanacheMongoRepository<Channel> {

    /**
     * Finds a channel by its unique code identifier.
     * <p>
     * This method performs a reactive query to find the first Channel document
     * that matches the specified code.
     *
     * @param code The unique channel code to search for
     * @return A {@link Uni} emitting the found Channel or null if not found
     */
    public Uni<Channel> findByCode(String code) {
        return find("code", code).firstResult();
    }
}