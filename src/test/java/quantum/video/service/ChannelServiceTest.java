package quantum.video.service;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.quarkus.panache.common.Page;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Answers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import quantum.video.model.Channel;
import quantum.video.model.Program;
import quantum.video.model.PagedData;
import quantum.video.repository.ChannelRepository;
import quantum.video.repository.ProgramRepository;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @InjectMocks
    ChannelService channelService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ChannelRepository repository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ProgramRepository programRepository;

    @Test
    void testGetPlaybackChannels_NoChannels() {
        int level = 5, page = 0, size = 10;
        when(repository.find(anyString(), anyInt()).page(any(Page.class)).list())
            .thenReturn(Uni.createFrom().item(Collections.emptyList()));
        when(repository.count(anyString(), anyInt()))
            .thenReturn(Uni.createFrom().item(0L));

        PagedData<Tuple2<Channel, Program>> result = channelService
            .getPlaybackChannels(level, page, size)
            .await().indefinitely();

        assertTrue(result.items().isEmpty());
        assertEquals(page, result.page());
        assertEquals(size, result.size());
        assertEquals(0L, result.elements());
        assertEquals(0, result.total());
    }

    @Test
    void testGetPlaybackChannels_WithChannels() {
        int level = 5, page = 1, size = 2;
        Channel ch1 = new Channel(); ch1.code = "ch1"; ch1.id = new ObjectId("60d5f484b3f1c8b1a4e8e0a1");
        Channel ch2 = new Channel(); ch2.code = "ch2"; ch2.id = new ObjectId("60d5f484b3f1c8b1a4e8e0a2");
        List<Channel> channels = List.of(ch1, ch2);

        when(repository.find(anyString(), anyInt()).page(any(Page.class)).list())
            .thenReturn(Uni.createFrom().item(channels));
        when(repository.count(anyString(), anyInt()))
            .thenReturn(Uni.createFrom().item(2L));

        AtomicInteger callCount = new AtomicInteger(0);
        when(programRepository.find(anyString(), any(ObjectId.class), any(Instant.class)).firstResult())
            .thenAnswer(invocation -> {
                if (callCount.incrementAndGet() == 1) {
                    Program prog = new Program(); prog.url = "ch1-url";
                    return Uni.createFrom().item(prog);
                }
                Program prog = new Program(); prog.url = "ch2-url";
                return Uni.createFrom().item(prog);
            });

        PagedData<Tuple2<Channel, Program>> result = channelService
            .getPlaybackChannels(level, page, size)
            .await().indefinitely();

        List<Tuple2<Channel, Program>> data = result.items();
        assertEquals(2, data.size());
        assertEquals(ch1, data.get(0).getItem1());
        assertEquals("ch1-url", data.get(0).getItem2().url);
        assertEquals(ch2, data.get(1).getItem1());
        assertEquals("ch2-url", data.get(1).getItem2().url);

        assertEquals(page, result.page());
        assertEquals(size, result.size());
        assertEquals(2L, result.elements());
        assertEquals(1, result.total());
    }
}
