package quantum.video.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import quantum.video.model.Program;

/**
 * Repository for managing Program entities in MongoDB.
 * <p>
 * This repository provides reactive access to Program documents stored in MongoDB,
 * leveraging Quarkus Panache for simplified data access patterns. It enables non-blocking
 * CRUD operations and custom queries on Program entities.
 * <p>
 * The repository is application-scoped to ensure a single instance exists per application.
 *
 * @see Program
 * @see ReactivePanacheMongoRepository
 */
@ApplicationScoped
public class ProgramRepository implements ReactivePanacheMongoRepository<Program> {
}