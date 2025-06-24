package quantum.video.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import quantum.video.model.Program;


@ApplicationScoped
public class ProgramRepository implements ReactivePanacheMongoRepository<Program> {
}