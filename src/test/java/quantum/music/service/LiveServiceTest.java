package quantum.music.service;

import io.quarkus.cache.Cache;
import io.smallrye.mutiny.Uni;

import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import quantum.music.model.Channel;

import java.util.Base64;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class LiveServiceTest {

    @InjectMocks
    private LiveService liveService = new LiveService();
    private AutoCloseable closeable;
    @Mock
    private Cache cache;

    @BeforeEach
    void setUp() {
        closeable = openMocks(this);
        when(cache.get(anyString(), any())).thenAnswer(invocation -> {
            String channelCode = invocation.getArgument(0);
            Function<String, Channel> function = invocation.getArgument(1);
            return Uni.createFrom().item(function.apply(channelCode));
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    void testGetMPDUrl_ChannelFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String channelCode = "ch1";
        Channel mockChannel = new Channel();
        mockChannel.url = "https://domain.com/path/to/ch1.mpd";

        try (MockedStatic<Channel> mocked = mockStatic(Channel.class)) {
            mocked.when(() -> Channel.findByCode(channelCode)).thenReturn(mockChannel);

            Uni<String> result = liveService.getMPDUrl(host, token, channelCode);
            assertEquals("https://domain.com/token123//path/to/ch1.mpd", result.await().indefinitely());
        }
    }

    @Test
    void testGetMPDUrl_ChannelNotFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String channelCode = "notfound";

        try (MockedStatic<Channel> mocked = mockStatic(Channel.class)) {
            mocked.when(() -> Channel.findByCode(channelCode)).thenReturn(null);

            Uni<String> result = liveService.getMPDUrl(host, token, channelCode);
            assertThrows(Exception.class, () -> result.await().indefinitely());
        }
    }

    @Test
    void testGetVideoUrl_ChannelFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String channelCode = "ch1";
        String file = "seg1";
        Channel mockChannel = new Channel();
        mockChannel.url = "https://domain.com/path/to/ch1.mpd";

        try (MockedStatic<Channel> mocked = mockStatic(Channel.class)) {
            mocked.when(() -> Channel.findByCode(channelCode)).thenReturn(mockChannel);

            Uni<String> result = liveService.getVideoUrl(host, token, channelCode, file);
            assertEquals("https://domain.com/token123//path/to/ch1-avc1_seg1.mp4", result.await().indefinitely());
        }
    }

    @Test
    void testGetVideoUrl_ChannelNotFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String channelCode = "ch1";
        String file = "seg1";

        try (MockedStatic<Channel> mocked = mockStatic(Channel.class)) {
            mocked.when(() -> Channel.findByCode(channelCode)).thenReturn(null);
            Uni<String> result = liveService.getVideoUrl(host, token, channelCode, file);
            assertThrows(Exception.class, () -> result.await().indefinitely());
        }
    }

    @Test
    void testGetAudioUrl_ChannelFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String channelCode = "ch1";
        String file = "seg1";
        Channel mockChannel = new Channel();
        mockChannel.url = "https://domain.com/path/to/ch1.mpd";

        try (MockedStatic<Channel> mocked = mockStatic(Channel.class)) {
            mocked.when(() -> Channel.findByCode(channelCode)).thenReturn(mockChannel);

            Uni<String> result = liveService.getAudioUrl(host, token, channelCode, file);
            assertEquals("https://domain.com/token123//path/to/ch1-mp4a_seg1.mp4", result.await().indefinitely());
        }
    }

    @Test
    void testGetAudioUrl_ChannelNotFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String channelCode = "ch1";
        String file = "seg1";

        try (MockedStatic<Channel> mocked = mockStatic(Channel.class)) {
            mocked.when(() -> Channel.findByCode(channelCode)).thenReturn(null);

            Uni<String> result = liveService.getAudioUrl(host, token, channelCode, file);
            assertThrows(Exception.class, () -> result.await().indefinitely());
        }
    }
}
