package quantum.video.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
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
}