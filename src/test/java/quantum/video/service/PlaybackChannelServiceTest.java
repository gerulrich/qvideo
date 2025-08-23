package quantum.video.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.video.model.Channel;
import quantum.video.model.PagedData;
import quantum.video.model.Program;
import quantum.video.repository.ChannelRepository;
import quantum.video.repository.ProgramRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaybackChannelServiceTest {

    public static final String CHANNEL_ID_1 = "60d5f484b3f1c8b1a4e8e0a1";
    public static final String CHANNEL_ID_2 = "60d5f484b3f1c8b1a4e8e0a2";
    @InjectMocks
    PlaybackChannelService channelService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ChannelRepository repository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ProgramRepository programRepository;

    @Test
    void testGetPlaybackChannels_emptyList() {
        when(repository.find(anyString(), anyInt()).page(any(Page.class)).list()).
        thenReturn(
            Uni.createFrom().item(Collections.emptyList())
        );

        when(repository.count(anyString(), anyInt())).
        thenReturn(
            Uni.createFrom().item(0L)
        );

        int level = 5, page = 0, size = 10;
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
    void testGetPlaybackChannels() {
        when(repository.find(anyString(), anyInt()).page(any(Page.class)).list()).
        thenReturn(
            Uni.createFrom().item(List.of(
                    buildChannel(CHANNEL_ID_1, "ch1"),
                    buildChannel(CHANNEL_ID_2, "ch2")
            ))
        );

        when(repository.count(anyString(), anyInt())).
        thenReturn(
            Uni.createFrom().item(2L)
        );

        when(programRepository.find(anyString(), any(ObjectId.class), any(Instant.class))).
        thenAnswer(
            this::getReactivePanacheQueryForProgram
        );

        int level = 5, page = 1, size = 2;
        PagedData<Tuple2<Channel, Program>> result = channelService
            .getPlaybackChannels(level, page, size)
            .await().indefinitely();

        List<Tuple2<Channel, Program>> data = result.items();
        assertEquals(2, data.size());

        Channel ch1 = data.getFirst().getItem1();
        Program pgr1 = data.getFirst().getItem2();
        assertEquals(CHANNEL_ID_1, ch1.id.toString());
        assertEquals("ch1-url", pgr1.url);

        Channel ch2 = data.get(1).getItem1();
        Program pgr2 = data.get(1).getItem2();
        assertEquals(CHANNEL_ID_2, ch2.id.toString());
        assertEquals("ch2-url", pgr2.url);

        assertEquals(page, result.page());
        assertEquals(size, result.size());
        assertEquals(2L, result.elements());
        assertEquals(1, result.total());
    }

    @Test
    void testGetPlaybackChannels_NoProgramForChannel() {
        when(repository.find(anyString(), anyInt()).page(any(Page.class)).list()).
        thenReturn(
            Uni.createFrom().item(List.of(
                buildChannel(CHANNEL_ID_1, "ch1"),
                buildChannel(CHANNEL_ID_2, "ch2")
            ))
        );

        when(repository.count(anyString(), anyInt())).
        thenReturn(
            Uni.createFrom().item(2L)
        );

        when(programRepository.find(anyString(), any(ObjectId.class), any(Instant.class))).
        thenAnswer( invocation -> {
            @SuppressWarnings("unchecked") ReactivePanacheQuery<Program> query = mock(ReactivePanacheQuery.class);
            when(query.firstResult()).thenReturn(Uni.createFrom().nullItem());
            return query;
        });

        int level = 5, page = 1, size = 2;
        PagedData<Tuple2<Channel, Program>> result = channelService
                .getPlaybackChannels(level, page, size)
                .await().indefinitely();

        List<Tuple2<Channel, Program>> data = result.items();
        assertEquals(2, data.size());

        Channel ch1 = data.getFirst().getItem1();
        Program pgr1 = data.getFirst().getItem2();
        assertEquals(CHANNEL_ID_1, ch1.id.toString());
        assertNull(pgr1);

        Channel ch2 = data.get(1).getItem1();
        Program pgr2 = data.get(1).getItem2();
        assertEquals(CHANNEL_ID_2, ch2.id.toString());
        assertNull(pgr2);

        assertEquals(page, result.page());
        assertEquals(size, result.size());
        assertEquals(2L, result.elements());
        assertEquals(1, result.total());
    }

    @Test
    void testGetPlaybackChannel() {
        when(repository.find(anyString(), any(ObjectId.class), anyInt()).firstResult()).
        thenReturn(
            Uni.createFrom().item(buildChannel(CHANNEL_ID_1, "ch1"))
        );

        when(programRepository.find(anyString(), any(ObjectId.class), any(Instant.class))).
        thenAnswer( invocation -> {
            @SuppressWarnings("unchecked") ReactivePanacheQuery<Program> query = mock(ReactivePanacheQuery.class);
            when(query.firstResult()).thenReturn(Uni.createFrom().nullItem());
            return query;
        });

        Tuple2<Channel, Program> result = channelService
                .getPlaybackChannel(CHANNEL_ID_1, 1)
                .await().indefinitely();

        Channel channel = result.getItem1();
        Program program = result.getItem2();
        assertNotNull(channel);
        assertNull(program);
    }

    @Test
    void testGetPlaybackChannel_NoProgramForChannel() {
        when(repository.find(anyString(), any(ObjectId.class), anyInt()).firstResult()).
        thenReturn(
            Uni.createFrom().item(buildChannel(CHANNEL_ID_1, "ch1"))
        );

        when(programRepository.find(anyString(), any(ObjectId.class), any(Instant.class))).
        thenAnswer(
            this::getReactivePanacheQueryForProgram
        );

        Tuple2<Channel, Program> result = channelService
                .getPlaybackChannel(CHANNEL_ID_1, 1)
                .await().indefinitely();

        Channel channel = result.getItem1();
        Program program = result.getItem2();
        assertNotNull(channel);
        assertNotNull(program);
    }

    private static Channel buildChannel(String id, String code) {
        Channel channel = new Channel();
        channel.id = new ObjectId(id);
        return channel;
    }

    private ReactivePanacheQuery<Program> getReactivePanacheQueryForProgram(InvocationOnMock invocation) {
        String id = invocation.getArgument(1).toString();
        Program prog = new Program();
        if (CHANNEL_ID_1.equals(id)) {
            prog.url = "ch1-url";
        } else {
            prog.url = "ch2-url";
        }
        @SuppressWarnings("unchecked") ReactivePanacheQuery<Program> query = mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(prog));
        return query;
    }

}