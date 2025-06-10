package quantum.music.service;


import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.bson.types.ObjectId;
import quantum.music.model.PagedData;
import quantum.music.model.Channel;
import quantum.music.model.Program;
import quantum.music.repository.ChannelRepository;
import quantum.music.repository.ProgramRepository;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class ChannelService {

    private static final String CHANNEL_QUERY = "{ 'level': {'$lt': ?1}, 'enabled': true }";
    private static final String CHANNEL_BY_ID_QUERY = "{ '_id': ?1, 'level': {'$lt': ?2}, 'enabled': true }";
    private static final String PROGRAM_QUERY = "{ 'channel': ?1, 'start': { '$lt': ?2 }, 'end': { '$gt': ?2 } }";

    @Inject
    ChannelRepository repository;

    @Inject
    ProgramRepository programRepository;

    public Uni<PagedData<Tuple2<Channel, Program>>> getPlaybackChannels(int level, int page, int size) {
        return Uni.combine().all().unis(
            repository.find(CHANNEL_QUERY, level).page(Page.of(page, size)).list(),
            repository.count(CHANNEL_QUERY, level)
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

    public Uni<Tuple2<Channel, Program>> getPlaybackChannel(String id, int level) {
        return repository.find(CHANNEL_BY_ID_QUERY, new ObjectId(id), level)
            .firstResult()
            .onItem().ifNull().failWith(() -> new NotFoundException("Channel not found"))
            .flatMap(this::getProgramByChannel);
    }

    private List<Uni<Tuple2<Channel, Program>>> getProgramsByChannel(List<Channel> channels) {
        return channels.stream()
                .map(this::getProgramByChannel)
                .toList();
    }

    private Uni<Tuple2<Channel, Program>> getProgramByChannel(Channel channel) {
        return programRepository.find(PROGRAM_QUERY, channel.id, Instant.now())
            .firstResult()
            .map(program -> Tuple2.of(channel, program));
    }
}

