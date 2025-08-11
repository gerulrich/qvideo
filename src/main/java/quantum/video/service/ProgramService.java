package quantum.video.service;


import org.bson.types.ObjectId;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import quantum.video.model.PagedData;
import quantum.video.model.Program;
import quantum.video.repository.ProgramRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

/**
 * Service class for managing program-related operations in the video streaming system.
 * 
 * <p>This service provides functionality to retrieve programs with pagination support
 * and individual program lookup by ID. All operations are reactive using Mutiny's Uni
 * for non-blocking execution.</p>
 * 
 * <p>The service filters programs based on a level threshold, ensuring only programs
 * with plan levels below the specified threshold are returned.</p>
 * 
 * @author Quantum Video Team
 * @since 1.0.0
 */
@ApplicationScoped
public class ProgramService {

    /**
     * MongoDB query to filter programs by plan level.
     * Returns programs where the plan level is less than the specified threshold.
     */
    private static final String PROGRAM_QUERY = "{ 'plan.level': {'$lt': ?1}}";
    
    /**
     * MongoDB query to find a specific program by ID and plan level.
     * Returns a program with the given ID where the plan level is less than the specified threshold.
     */
    private static final String PROGRAM_BY_ID_QUERY = "{ '_id': ?1, 'plan.level': {'$lt': ?2}}";

    /**
     * Repository for program data access operations.
     */
    @Inject
    ProgramRepository repository;

    /**
     * Retrieves a paginated list of programs filtered by plan level.
     * 
     * <p>This method returns programs where the plan level is less than the specified
     * threshold, with support for pagination. The result includes both the program
     * list and metadata about the pagination.</p>
     * 
     * @param level the maximum plan level threshold (exclusive) for filtering programs
     * @param page the page number (0-based indexing)
     * @param size the number of programs per page
     * @return a Uni containing the paginated program data with metadata
     * @throws IllegalArgumentException if page or size parameters are negative
     */
    public Uni<PagedData<Program>> getPrograms(int level, int page, int size) {
        return Uni.combine().all().unis(
            repository.find(PROGRAM_QUERY, level).page(Page.of(page, size)).list(),
            repository.count(PROGRAM_QUERY, level)
        )
        .asTuple()
        .map(tuple -> new PagedData<>(tuple.getItem1(), page, size, tuple.getItem2()));
    }

    /**
     * Retrieves a specific program by its ID, filtered by plan level.
     * 
     * <p>This method finds a program with the specified ID where the plan level
     * is less than the given threshold. If no such program exists, a
     * NotFoundException is thrown.</p>
     * 
     * @param id the MongoDB ObjectId of the program to retrieve
     * @param level the maximum plan level threshold (exclusive) for filtering
     * @return a Uni containing the found program
     * @throws NotFoundException if no program is found with the given ID and level criteria
     * @throws IllegalArgumentException if the provided ID is not a valid ObjectId format
     */
    public Uni<Program> getProgram(String id, int level) {
        return repository.find(PROGRAM_BY_ID_QUERY, new ObjectId(id), level)
            .firstResult()
            .onItem().ifNull().failWith(() -> new NotFoundException("Program not found"));
    }
}



