package quantum.video.service;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.bson.types.ObjectId;
import quantum.video.model.PagedData;
import quantum.video.model.Channel;
import quantum.video.model.Program;
import quantum.video.repository.ChannelRepository;
import quantum.video.repository.ProgramRepository;

import java.time.Instant;
import java.util.List;

/**
 * Service for managing playback channels and their associated programs.
 * <p>
 * This service provides methods to retrieve playback channels with their current
 * programs based on user authorization level. It handles pagination for channel
 * listings and provides detailed channel information with the currently playing program.
 * </p>
 * <p>
 * The service uses MongoDB queries with user authorization level filtering to ensure
 * users can only access channels they have permission to view. It leverages Mutiny's
 * reactive types for asynchronous, non-blocking operations and efficient handling of
 * database queries.
 * </p>
 * <p>
 * Key functionalities include:
 * <ul>
 *   <li>Retrieving paginated lists of playback channels with current programs</li>
 *   <li>Fetching specific channel details with authorization checks</li>
 *   <li>Determining the currently playing program for channels</li>
 * </ul>
 * </p>
 */
@ApplicationScoped
public class PlaybackChannelService {

    /**
     * MongoDB query for retrieving enabled channels below the specified authorization level.
     */
    private static final String PLAYBACK_CHANNEL_QUERY = "{ 'level': {'$lt': ?1}, 'enabled': true }";

    /**
     * MongoDB query for retrieving a specific channel by ID with authorization level check.
     */
    private static final String PLAYBACK_CHANNEL_BY_ID_QUERY = "{ '_id': ?1, 'level': {'$lt': ?2}, 'enabled': true }";

    /**
     * MongoDB query for finding the current program by channel ID and current timestamp.
     */
    private static final String CURRENT_PROGRAM_QUERY = "{ 'channel': ?1, 'start': { '$lt': ?2 }, 'end': { '$gt': ?2 } }";

    /**
     * Repository for channel data operations.
     */
    @Inject
    ChannelRepository channelRepository;

    /**
     * Repository for program data operations.
     */
    @Inject
    ProgramRepository programRepository;

    /**
     * Retrieves a paginated list of playback channels with their current programs.
     * <p>
     * This method returns channels that match the user's authorization level and
     * are enabled. Each channel is paired with its currently playing program.
     * The method uses a reactive approach to efficiently fetch both channels and
     * their associated programs, combining them into pairs.
     * </p>
     * <p>
     * If no channels are found for the given criteria, an empty paged data object
     * is returned.
     * </p>
     *
     * @param level The user's authorization level
     * @param page The page number (0-based) for pagination
     * @param size The number of items per page
     * @return A {@link Uni} containing paginated channel-program pairs wrapped in a {@link PagedData} object
     */
    public Uni<PagedData<Tuple2<Channel, Program>>> getPlaybackChannels(int level, int page, int size) {
        return Uni.combine().all().unis(
            channelRepository.find(PLAYBACK_CHANNEL_QUERY, level).page(Page.of(page, size)).list(),
            channelRepository.count(PLAYBACK_CHANNEL_QUERY, level)
        )
        .asTuple()
        .flatMap(tuple -> {
            List<Channel> channels = tuple.getItem1();
            long count = tuple.getItem2();
            if (channels.isEmpty()) {
                return Uni.createFrom().item(new PagedData<>(page, size, count));
            }
            return Uni.join().all(getProgramsByChannel(channels))
                .andCollectFailures()
                .map(data -> new PagedData<>(data, page, size, count));
        });
    }

    /**
     * Retrieves a specific playback channel with its current program.
     * <p>
     * This method returns detailed information about a specific channel and
     * its currently playing program, if the user has sufficient authorization level.
     * </p>
     * <p>
     * The method first verifies that the channel exists and the user has permission
     * to access it. If either condition fails, a NotFoundException is thrown.
     * </p>
     *
     * @param id The channel identifier (MongoDB ObjectId as string)
     * @param level The user's authorization level
     * @return A {@link Uni} containing the channel-program pair as a {@link Tuple2}
     * @throws NotFoundException if the channel is not found or the user lacks permission
     */
    public Uni<Tuple2<Channel, Program>> getPlaybackChannel(String id, int level) {
        return channelRepository.find(PLAYBACK_CHANNEL_BY_ID_QUERY, new ObjectId(id), level)
            .firstResult()
            .onItem().ifNull().failWith(() -> new NotFoundException("Channel not found"))
            .flatMap(this::getProgramByChannel);
    }

    /**
     * Creates a list of Uni tasks for retrieving programs for multiple channels.
     * <p>
     * This private helper method maps each channel to a task that retrieves
     * its currently playing program.
     * </p>
     *
     * @param channels The list of channels to get programs for
     * @return A list of {@link Uni} tasks that will resolve to channel-program pairs
     */
    private List<Uni<Tuple2<Channel, Program>>> getProgramsByChannel(List<Channel> channels) {
        return channels.stream()
                .map(this::getProgramByChannel)
                .toList();
    }

    /**
     * Retrieves the currently playing program for a specific channel.
     * <p>
     * This private helper method queries the program repository to find the program
     * that is currently playing (where current time is between start and end times)
     * for the given channel.
     * </p>
     *
     * @param channel The channel to get the current program for
     * @return A {@link Uni} containing a {@link Tuple2} with the channel and its current program
     */
    private Uni<Tuple2<Channel, Program>> getProgramByChannel(Channel channel) {
        return programRepository.find(CURRENT_PROGRAM_QUERY, channel.id, Instant.now())
            .firstResult()
            .map(program -> Tuple2.of(channel, program));
    }
}
