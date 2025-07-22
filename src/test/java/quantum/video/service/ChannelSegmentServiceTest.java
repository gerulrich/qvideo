package quantum.video.service;

import io.quarkus.cache.Cache;
import io.smallrye.mutiny.Uni;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    void testGetVideoSegmentFound() {
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

        Uni<String> result = service.getVideoSegment(host, token, code, file);
        assertEquals("https://domain.com/token123/path/to/ch1-avc1_seg1.mp4", result.await().indefinitely());
    }

    @Test
    void testGetVideoSegmentNotFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String channelCode = "ch1";
        String file = "seg1";

        when(cache.get(anyString(), any())).thenReturn(Uni.createFrom().item(Uni.createFrom().nullItem()));
        Uni<String> result = service.getVideoSegment(host, token, channelCode, file);
        assertThrows(Exception.class, () -> result.await().indefinitely());
    }

    @Test
    void testGetAudioUrl_ChannelFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String code = "ch1";
        String file = "seg1";

        when(cache.get(anyString(), any())).thenAnswer(invocation -> {
            Channel channel = new Channel();
            channel.url = "https://domain.com/path/to/ch1.mpd";
            channel.code = code;
            return Uni.createFrom().item(Uni.createFrom().item(channel));
        });


        Uni<String> result = service.getAudioSegment(host, token, code, file);
        assertEquals("https://domain.com/token123//path/to/ch1-mp4a_seg1.mp4", result.await().indefinitely());
    }

    @Test
    void testGetAudioUrl_ChannelNotFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String channelCode = "ch1";
        String file = "seg1";

        when(cache.get(anyString(), any())).thenReturn(Uni.createFrom().item(Uni.createFrom().nullItem()));
        Uni<String> result = service.getAudioSegment(host, token, channelCode, file);
        assertThrows(Exception.class, () -> result.await().indefinitely());
    }
}
