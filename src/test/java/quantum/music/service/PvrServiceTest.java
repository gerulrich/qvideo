package quantum.music.service;

import io.quarkus.cache.Cache;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.music.model.Program;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PvrServiceTest {

    @InjectMocks
    private PvrService pvrService;
    @Mock
    private Cache cache;


    @Test
    void testGetMPDUrl_ProgramFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123";
        String id = "66ed71d0174ce2b912555115";
        String code = "ch1";

        when(cache.get(anyString(), any())).thenAnswer(invocation -> {
            Program program = new Program();
            program.url = "https://domain.com/path/to/ch1.mpd";
            return Uni.createFrom().item(Uni.createFrom().item(program));
        });

        Uni<String> result = pvrService.getMPDUrl(host, token, id, code);
        assertEquals("https://domain.com/token123/path/to/ch1.mpd", result.await().indefinitely());

    }

    @Test
    void testGetMPDUrl_ProgramNotFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String id = "notfound";
        String channel = "ch1";

        when(cache.get(anyString(), any())).thenReturn(Uni.createFrom().item(Uni.createFrom().nullItem()));
        Uni<String> result = pvrService.getMPDUrl(host, token, id, channel);
        assertThrows(Exception.class, () -> result.await().indefinitely());

    }

    @Test
    void testGetVideoUrl_ProgramFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String file = "seg1";
        String id = "66ed71d0174ce2b912555115";
        String code = "ch1";

        when(cache.get(anyString(), any())).thenAnswer(invocation -> {
            Program program = new Program();
            program.url = "https://domain.com/path/to/ch1.mpd";
            return Uni.createFrom().item(Uni.createFrom().item(program));
        });

        Uni<String> result = pvrService.getVideoUrl(host, token, id, code, file);
        assertEquals("https://domain.com/token123//path/to/ch1-avc1_seg1.mp4", result.await().indefinitely());

    }

    @Test
    void testGetVideoUrl_ProgramNotFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String id = "notfound";
        String channel = "ch1";
        String file = "seg1";

        when(cache.get(anyString(), any())).thenReturn(Uni.createFrom().item(Uni.createFrom().nullItem()));
        Uni<String> result = pvrService.getVideoUrl(host, token, id, channel, file);
        assertThrows(Exception.class, () -> result.await().indefinitely());

    }

    @Test
    void testGetAudioUrl_ProgramFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String file = "seg1";

        String id = "66ed71d0174ce2b912555115";
        String code = "ch1";

        when(cache.get(anyString(), any())).thenAnswer(invocation -> {
            Program program = new Program();
            program.url = "https://domain.com/path/to/ch1.mpd";
            return Uni.createFrom().item(Uni.createFrom().item(program));
        });

        Uni<String> result = pvrService.getAudioUrl(host, token, id, code, file);
        assertEquals("https://domain.com/token123//path/to/ch1-mp4a_seg1.mp4", result.await().indefinitely());

    }

    @Test
    void testGetAudioUrl_ProgramNotFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String id = "notfound";
        String channel = "ch1";
        String file = "seg1";

        when(cache.get(anyString(), any())).thenReturn(Uni.createFrom().item(Uni.createFrom().nullItem()));
        Uni<String> result = pvrService.getAudioUrl(host, token, id, channel, file);
        assertThrows(Exception.class, () -> result.await().indefinitely());

    }
}
