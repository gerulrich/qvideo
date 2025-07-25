package quantum.video.service;

import io.quarkus.cache.Cache;
import io.smallrye.mutiny.Uni;

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.video.builder.TestBuilder;
import quantum.video.model.Channel;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelSegmentServiceTest {

    @InjectMocks
    private ChannelSegmentService service;
    @Mock
    private Cache cache;

    @Test
    @DisplayName("Should return video segment URL when channel is found")
    void getVideoSegmentFound_returnsUrl() {
        // Given
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123";
        String code = "ch1";
        String file = "seg1";

        when(cache.get(anyString(), any())).thenAnswer(invocation -> {
            Channel channel = new Channel();
            channel.url = "https://domain.com/path/to/ch1.mpd";
            channel.code = code;
            return Uni.createFrom().item(Uni.createFrom().item(channel));
        });

        // When
        Uni<String> result = service.getVideoSegment(host, token, code, file);

        // Then
        assertEquals("https://domain.com/token123/path/to/ch1-avc1_seg1.mp4", result.await().indefinitely());

        // Verify
        verify(cache).get(anyString(), any());
        verifyNoMoreInteractions(cache);
    }

    @Test
    @DisplayName("Should throw exception when channel is not found for video segment")
    void getVideoSegmentNotFound_throwsException() {
        // Given
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123";
        String code = "ch1";
        String file = "seg1";

        when(cache.get(anyString(), any())).thenReturn(Uni.createFrom().nullItem());

        // When & Then
        assertThrows(RuntimeException.class, () -> service.getVideoSegment(host, token, code, file).await().indefinitely());

        // Verify
        verify(cache).get(anyString(), any());
        verifyNoMoreInteractions(cache);
    }

    @Test
    void testGetAudioUrl_ChannelFound() {
        // Given
        when(cache.get(anyString(), any())).thenReturn(
            Uni.createFrom().item(Uni.createFrom().item(
                newChannel()
                    .id("60d5f484b3f1c8b1a4e8e0a1")
                    .url("https://domain.com/path/to/ch1.mpd")
                    .code("ch1")
                    .build()
            ))
        );

        // When & Then
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        service.getAudioSegment(host, "token123", "ch1", "seg1")
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertItem("https://domain.com/token123/path/to/ch1-mp4a_seg1.mp4");

        // Verify
        verify(cache).get(eq("ch1"), any());
        verifyNoMoreInteractions(cache);

    }

    @Test
    void testGetAudioUrl_ChannelNotFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String channelCode = "ch1";
        String file = "seg1";

        when(cache.get(anyString(), any())).thenReturn(Uni.createFrom().item(Uni.createFrom().nullItem()));
        service.getAudioSegment(host, token, channelCode, file)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        verify(cache).get(eq("ch1"), any());
        verifyNoMoreInteractions(cache);
    }

    protected TestBuilder.ChannelBuilder newChannel() {
        return new TestBuilder.ChannelBuilder();
    }
}
