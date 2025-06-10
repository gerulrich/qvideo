package quantum.music.service;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpClientResponse;
import jakarta.ws.rs.WebApplicationException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.music.model.Channel;
import quantum.music.model.Program;
import quantum.music.repository.ChannelRepository;
import quantum.music.repository.ProgramRepository;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamServiceTest {

    @InjectMocks
    private StreamService streamService;
    @Mock
    private HttpClient httpClient;
    @Mock
    private ChannelRepository repository;
    @Mock
    private ProgramRepository programRepository;
    @Mock
    private Vertx vertx;

    @BeforeEach
    void setUp() {
        when(vertx.createHttpClient()).thenReturn(httpClient);
        streamService.init();
    }

    @Test
    void testGetPvrManifestUrl_ProgramFound() {
        String id = "66ed71d0174ce2b912555115";
        String channel = "ch1";
        Program mockProgram = new Program();
        mockProgram.url = "https://domain.com/path/to/ch1.mpd";

        HttpClientRequest request = mock(HttpClientRequest.class);
        HttpClientResponse response = mock(HttpClientResponse.class);

        when(httpClient.request(any())).thenReturn(Uni.createFrom().item(request));
        when(request.send()).thenReturn(Uni.createFrom().item(response));
        when(response.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("Location", "https://domain.com/token123/path/to/ch1.mpd"));
        when(programRepository.findById(new ObjectId(id))).thenReturn(Uni.createFrom().item(mockProgram));

        String expectedUrl = String.format("/pvr/%s/%s/%s/a/b/c/d/e/%s.mpd",
                    Base64.getEncoder().encodeToString("domain.com".getBytes()), "token123", id, channel);

        Uni<String> result = streamService.getPvrManifestUrl(id, channel);
        assertEquals(expectedUrl, result.await().indefinitely());
    }

    @Test
    void testGetPvrManifestUrl_ProgramNotFound() {
        String id = "66ed71d0174ce2b912555115";
        String channel = "ch1";

        when(programRepository.findById(any())).thenReturn(Uni.createFrom().failure(new WebApplicationException("Channel not found")));
        Uni<String> result = streamService.getPvrManifestUrl(id, channel);
        assertThrows(WebApplicationException.class, () -> result.await().indefinitely());

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

        when(repository.findByCode(channelCode)).thenReturn(Uni.createFrom().item(mockChannel));
        String expectedUrl = String.format("/live/%s/%s/%s.mpd", Base64.getEncoder().encodeToString("domain.com".getBytes()), "token123", channelCode);

        Uni<String> result = streamService.getManifestRedirect(channelCode);
        assertEquals(expectedUrl, result.await().indefinitely());
    }

    @Test
    void testGetManifestRedirect_ChannelNotFound() {
        String channelCode = "notfound";
        when(repository.findByCode(channelCode)).thenReturn(Uni.createFrom().failure(new WebApplicationException("Channel not found")));
        Uni<String> result = streamService.getManifestRedirect(channelCode);
        assertThrows(WebApplicationException.class, () -> result.await().indefinitely());
    }
}
