package quantum.music.service;

import io.quarkus.cache.Cache;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import quantum.music.model.Program;

import java.util.Base64;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class PvrServiceTest {

    @InjectMocks
    private PvrService pvrService = new PvrService();
    private AutoCloseable closeable;
    @Mock
    private Cache cache;

    @BeforeEach
    void setUp() {
        closeable = openMocks(this);
        when(cache.get(anyString(), any())).thenAnswer(invocation -> {
            String channelCode = invocation.getArgument(0);
            Function<String, Program> function = invocation.getArgument(1);
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
    void testGetMPDUrl_ProgramFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String id = "prog1";
        String channel = "ch1";
        Program mockProgram = new Program();
        mockProgram.url = "https://domain.com/path/to/ch1.mpd";

        try (MockedStatic<Program> mocked = mockStatic(Program.class)) {
            mocked.when(() -> Program.findById(id)).thenReturn(mockProgram);

            Uni<String> result = pvrService.getMPDUrl(host, token, id, channel);
            assertEquals("https://domain.com/token123//path/to/ch1.mpd", result.await().indefinitely());
        }
    }

    @Test
    void testGetMPDUrl_ProgramNotFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String id = "notfound";
        String channel = "ch1";

        try (MockedStatic<Program> mocked = mockStatic(Program.class)) {
            mocked.when(() -> Program.findById(id)).thenReturn(null);

            Uni<String> result = pvrService.getMPDUrl(host, token, id, channel);
            assertThrows(Exception.class, () -> result.await().indefinitely());
        }
    }

    @Test
    void testGetVideoUrl_ProgramFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String id = "prog1";
        String channel = "ch1";
        String file = "seg1";
        Program mockProgram = new Program();
        mockProgram.url = "https://domain.com/path/to/ch1.mpd";

        try (MockedStatic<Program> mocked = mockStatic(Program.class)) {
            mocked.when(() -> Program.findById(id)).thenReturn(mockProgram);

            Uni<String> result = pvrService.getVideoUrl(host, token, id, channel, file);
            assertEquals("https://domain.com/token123//path/to/ch1-avc1_seg1.mp4", result.await().indefinitely());
        }
    }

    @Test
    void testGetVideoUrl_ProgramNotFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String id = "notfound";
        String channel = "ch1";
        String file = "seg1";

        try (MockedStatic<Program> mocked = mockStatic(Program.class)) {
            mocked.when(() -> Program.findById(id)).thenReturn(null);

            Uni<String> result = pvrService.getVideoUrl(host, token, id, channel, file);
            assertThrows(Exception.class, () -> result.await().indefinitely());
        }
    }

    @Test
    void testGetAudioUrl_ProgramFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String id = "prog1";
        String channel = "ch1";
        String file = "seg1";
        Program mockProgram = new Program();
        mockProgram.url = "https://domain.com/path/to/ch1.mpd";

        try (MockedStatic<Program> mocked = mockStatic(Program.class)) {
            mocked.when(() -> Program.findById(id)).thenReturn(mockProgram);

            Uni<String> result = pvrService.getAudioUrl(host, token, id, channel, file);
            assertEquals("https://domain.com/token123//path/to/ch1-mp4a_seg1.mp4", result.await().indefinitely());
        }
    }

    @Test
    void testGetAudioUrl_ProgramNotFound() {
        String host = Base64.getEncoder().encodeToString("domain.com".getBytes());
        String token = "token123/";
        String id = "notfound";
        String channel = "ch1";
        String file = "seg1";

        try (MockedStatic<Program> mocked = mockStatic(Program.class)) {
            mocked.when(() -> Program.findById(id)).thenReturn(null);

            Uni<String> result = pvrService.getAudioUrl(host, token, id, channel, file);
            assertThrows(Exception.class, () -> result.await().indefinitely());
        }
    }
}
