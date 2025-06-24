package quantum.video.service;


import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.bson.types.ObjectId;
import quantum.video.model.PagedData;
import quantum.video.model.Program;
import quantum.video.repository.ProgramRepository;

@ApplicationScoped
public class ProgramService {

    private static final String PROGRAM_QUERY = "{ 'plan.level': {'$lt': ?1}}";
    private static final String PROGRAM_BY_ID_QUERY = "{ '_id': ?1, 'plan.level': {'$lt': ?2}}";

    @Inject
    ProgramRepository repository;

    public Uni<PagedData<Program>> getPrograms(int level, int page, int size) {
        return Uni.combine().all().unis(
            repository.find(PROGRAM_QUERY, level).page(Page.of(page, size)).list(),
            repository.count(PROGRAM_QUERY, level)
        )
        .asTuple()
        .map(tuple -> new PagedData<>(tuple.getItem1(), page, size, tuple.getItem2()));
    }

    public Uni<Program> getProgram(String id, int level) {
        return repository.find(PROGRAM_BY_ID_QUERY, new ObjectId(id), level)
            .firstResult()
            .onItem().ifNull().failWith(() -> new NotFoundException("Program not found"));
    }
}



