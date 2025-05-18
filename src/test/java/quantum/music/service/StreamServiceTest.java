package quantum.music.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpClientResponse;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import quantum.music.model.Channel;
import quantum.music.model.Program;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StreamServiceTest {

    private StreamService streamService;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        Vertx vertx = mock(Vertx.class);
        httpClient = mock(HttpClient.class);
        when(vertx.createHttpClient()).thenReturn(httpClient);

        streamService = new StreamService(vertx);
        streamService.init();
    }

    @Test
    void testGetPvrManifestUrl_ProgramFound() {
        String id = "prog1";
        String channel = "ch1";
        Program mockProgram = new Program();
        mockProgram.url = "https://domain.com/path/to/ch1.mpd";

        HttpClientRequest request = mock(HttpClientRequest.class);
        HttpClientResponse response = mock(HttpClientResponse.class);

        when(httpClient.request(any())).thenReturn(Uni.createFrom().item(request));
        when(request.send()).thenReturn(Uni.createFrom().item(response));
        when(response.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("Location", "https://domain.com/token123/path/to/ch1.mpd"));

        try (MockedStatic<Program> mocked = mockStatic(Program.class)) {
            mocked.when(() -> Program.findById(id)).thenReturn(mockProgram);

            String expectedUrl = String.format("/pvr/%s/%s/%s/a/b/c/d/e/%s.mpd",
                    Base64.getEncoder().encodeToString("domain.com".getBytes()), "token123", id, channel);

            Uni<String> result = streamService.getPvrManifestUrl(id, channel);
            assertEquals(expectedUrl, result.await().indefinitely());
        }
    }

    @Test
    void testGetPvrManifestUrl_ProgramNotFound() {
        String id = "notfound";
        String channel = "ch1";

        try (MockedStatic<Program> mocked = mockStatic(Program.class)) {
            mocked.when(() -> Program.findById(id)).thenReturn(null);

            Uni<String> result = streamService.getPvrManifestUrl(id, channel);
            assertThrows(WebApplicationException.class, () -> result.await().indefinitely());
        }
    }

    @Test
    void testGetManifestRedirect_ChannelFound() {
        String channelCode = "ch1";
        Channel mockChannel = new Channel();
        mockChannel.url = "https://domain.com/path/to/ch1.mpd";
        mockChannel.code = channelCode;

        HttpClientRequest request = mock(HttpClientRequest.class);
        HttpClientResponse response = mock(HttpClientResponse.class);

        when(httpClient.request(any())).thenReturn(Uni.createFrom().item(request));
        when(request.send()).thenReturn(Uni.createFrom().item(response));
        when(response.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("Location", "https://domain.com/token123/path/to/ch1.mpd"));

        try (MockedStatic<Channel> mocked = mockStatic(Channel.class)) {
            mocked.when(() -> Channel.findByCode(channelCode)).thenReturn(mockChannel);

            String expectedUrl = String.format("/live/%s/%s/%s.mpd",
                    Base64.getEncoder().encodeToString("domain.com".getBytes()), "token123", channelCode);

            Uni<String> result = streamService.getManifestRedirect(channelCode);
            assertEquals(expectedUrl, result.await().indefinitely());
        }
    }

    @Test
    void testGetManifestRedirect_ChannelNotFound() {
        String channelCode = "notfound";

        try (MockedStatic<Channel> mocked = mockStatic(Channel.class)) {
            mocked.when(() -> Channel.findByCode(channelCode)).thenReturn(null);

            Uni<String> result = streamService.getManifestRedirect(channelCode);
            assertThrows(WebApplicationException.class, () -> result.await().indefinitely());
        }
    }

    @Test
    void testStream_Success() {
        String url = "https://domain.com/path/to/file";
        HttpClientRequest request = mock(HttpClientRequest.class);
        HttpClientResponse response = mock(HttpClientResponse.class);

        when(httpClient.request(any())).thenReturn(Uni.createFrom().item(request));
        when(request.send()).thenReturn(Uni.createFrom().item(response));
        when(response.statusCode()).thenReturn(200);

        Multi<Buffer> result = streamService.stream(url);
        assertNotNull(result);
    }
}
