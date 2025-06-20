package quantum.music.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import quantum.music.model.Program;


@ApplicationScoped
public class ProgramRepository implements ReactivePanacheMongoRepository<Program> {
}