package quantum.video.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.quarkus.cache.Cache;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpClientResponse;
import quantum.video.utils.BaseTestUtils;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;

@ExtendWith(MockitoExtension.class)
class ChannelManifestServiceTest extends BaseTestUtils {

    private static final String CHANNEL_URL = "https://qvideo.com/live/channel1.mpd";
    private static final String HOST = "cXZpZGVvLmNvbQ=="; // Base64 encoding of "qvideo.com"
    private static final String TOKEN = "token123";
    private static final ObjectId CHANNEL_ID = new ObjectId("60d5f484b3f1c8b1a4e8e0a1");

    @InjectMocks
    private ChannelManifestService service;
    @Mock
    private Cache cache;
    @Mock
    private StreamStrategy stream;
    @Mock
    private HttpClientRequest httpRequest;
    @Mock
    private HttpClientResponse httpResponse;

    @Test
    @DisplayName("Should return manifest redirect URL when channel is found")
    void getManifestRedirectUrl_returnsUrl() {
        // Given
        when(cache.get(any(), anyCacheLoader()))
        .thenReturn(mockCacheHit(
            newChannel()
                .id(CHANNEL_ID)
                .url(CHANNEL_URL)
                .name("Test Channel")
                .build()
        ));
        when(stream.get(anyString())).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send()).thenReturn(Uni.createFrom().item(httpResponse));
        when(httpResponse.headers()).thenReturn(
            newHeaders().add(HttpHeaders.LOCATION, "https://qvideo.com/token456/channel.mpd").build()
        );

        // When & Then
        service.getManifestRedirectUrl(CHANNEL_ID)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .assertItem("/live/cXZpZGVvLmNvbQ==/token456/60d5f484b3f1c8b1a4e8e0a1/channel1.mpd");

        // Verify
        verify(cache).get(eq(CHANNEL_ID), anyCacheLoader());
        verify(stream).get(CHANNEL_URL);
        verify(httpRequest).send();
        verifyNoMoreInteractions(cache, stream);
    }

    @Test
    @DisplayName("Should throw NotFoundException when channel is missing for manifest redirect")
    void getManifestRedirectUrl_channelNotFound_throwsException() {
        // Given
        when(cache.get(any(), anyCacheLoader())).thenReturn(mockCacheMiss());

        // When & Then
        service.getManifestRedirectUrl(CHANNEL_ID)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        // Verify
        verify(cache).get(eq(CHANNEL_ID), anyCacheLoader());
        verifyNoMoreInteractions(cache, stream);
    }

    @Test
    @DisplayName("Should handle HTTP request failure and retry")
    void getManifestRedirectUrl_httpRequestFailure_retries() {
        // Given
        when(cache.get(any(), anyCacheLoader()))
        .thenReturn(mockCacheHit(
            newChannel()
                .id(CHANNEL_ID)
                .url(CHANNEL_URL)
                .name("Test Channel")
                .build()
        ));
        when(stream.get(anyString())).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send())
            .thenReturn(Uni.createFrom().failure(new RuntimeException("Network error")))
            .thenReturn(Uni.createFrom().item(httpResponse));
        when(httpResponse.headers()).thenReturn(
            newHeaders().add(HttpHeaders.LOCATION, "https://qvideo.com/token456/channel.mpd").build()
        );

        // When & Then
        service.getManifestRedirectUrl(CHANNEL_ID)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .assertItem("/live/cXZpZGVvLmNvbQ==/token456/60d5f484b3f1c8b1a4e8e0a1/channel1.mpd");

        // Verify
        verify(cache).get(eq(CHANNEL_ID), anyCacheLoader());
        verify(stream).get(CHANNEL_URL);
        verify(httpRequest, org.mockito.Mockito.times(2)).send();
        verifyNoMoreInteractions(cache, stream);
    }

    @Test
    @DisplayName("Should handle malformed location header gracefully")
    void getManifestRedirectUrl_malformedLocationHeader_handlesGracefully() {
        // Given
        when(cache.get(any(), anyCacheLoader()))
        .thenReturn(mockCacheHit(
            newChannel()
                .id(CHANNEL_ID)
                .url(CHANNEL_URL)
                .name("Test Channel")
                .build()
        ));
        when(stream.get(anyString())).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send()).thenReturn(Uni.createFrom().item(httpResponse));
        when(httpResponse.headers()).thenReturn(newHeaders().add(HttpHeaders.LOCATION, "invalid-url-format").build());

        // When & Then
        service.getManifestRedirectUrl(CHANNEL_ID)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailed();

        // Verify
        verify(cache).get(eq(CHANNEL_ID), anyCacheLoader());
        verify(stream).get(CHANNEL_URL);
        verify(httpRequest).send();
        verifyNoMoreInteractions(cache, stream);
    }

    @Test
    @DisplayName("Should return manifest URL when channel is found")
    void getManifestUrl_returnsUrl() {
        // Given
        when(cache.get(any(), anyCacheLoader()))
        .thenReturn(mockCacheHit(
            newChannel()
                .url("https://qvideo.com/live/channel1.mpd")
                .name("Test Channel")
                .build()
        ));

        // When & Then
        service.getManifestUrl(HOST, TOKEN, CHANNEL_ID)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .assertItem("https://qvideo.com/token123/live/channel1.mpd");

        // Verify
        verify(cache).get(eq(CHANNEL_ID), anyCacheLoader());
        verifyNoMoreInteractions(cache, stream);
    }

    @Test
    @DisplayName("Should throw NotFoundException when channel is missing for manifest URL")
    void getManifestUrl_channelNotFound_throwsException() {
        // Given
        when(cache.get(any(), anyCacheLoader())).thenReturn(mockCacheMiss());

        // When & Then
        service.getManifestUrl(HOST, TOKEN, CHANNEL_ID)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        // Verify
        verify(cache).get(eq(CHANNEL_ID), anyCacheLoader());
        verifyNoMoreInteractions(cache, stream);
    }
}

