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
class ProgramManifestServiceTest extends BaseTestUtils {

    private static final String DOMAIN_BASE64 = "cXZpZGVvLmNvbQ=="; // Base64 encoding of "qvideo.com"
    private static final ObjectId PROGRAM_ID = new ObjectId("60d5f484b3f1c8b1a4e8e0a1");
    private static final String CHANNEL_CODE = "ch1";
    private static final String TOKEN = "token123";

    @InjectMocks
    private ProgramManifestService service;
    @Mock
    private Cache cache;
    @Mock
    private StreamStrategy stream;
    @Mock
    private HttpClientRequest httpRequest;
    @Mock
    private HttpClientResponse httpResponse;

    @Test
    @DisplayName("Should return manifest redirect URL when program is found")
    void getManifestRedirectUrlWithHost_returnsUrl() {
        // Given
        when(cache.get(any(), anyCacheLoader()))
        .thenReturn(mockCacheHit(
            newProgram()
                .id(PROGRAM_ID)
                .url("https://qvideo.com/path/to/program.mpd")
                .title("Test Program")
                .build()
        ));

        // When & Then
        service.getManifestRedirectUrl(DOMAIN_BASE64, TOKEN, PROGRAM_ID)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .assertItem("https://qvideo.com/token123/path/to/program.mpd");

        // Verify
        verify(cache).get(eq(PROGRAM_ID), anyCacheLoader());
        verifyNoMoreInteractions(cache);
    }

    @Test
    @DisplayName("Should throw NotFoundException when program is missing for manifest redirect")
    void getManifestRedirectUrlWithHost_programNotFound_throwsException() {
        // Given
        when(cache.get(any(), anyCacheLoader())).thenReturn(mockCacheMiss());

        // When & Then
        service.getManifestRedirectUrl(DOMAIN_BASE64, TOKEN, PROGRAM_ID)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        // Verify
        verify(cache).get(eq(PROGRAM_ID), anyCacheLoader());
        verifyNoMoreInteractions(cache);
    }

    @Test
    @DisplayName("Should return manifest redirect URL when program is found and HTTP request succeeds")
    void getManifestRedirectUrl_success() {
        // Given
        when(cache.get(any(), anyCacheLoader()))
        .thenReturn(mockCacheHit(
            newProgram()
                .id(PROGRAM_ID)
                .url("https://qvideo.com/path/to/program.mpd")
                .title("Test Program")
                .build()
        ));
        when(stream.get(anyString())).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send()).thenReturn(Uni.createFrom().item(httpResponse));
        io.vertx.mutiny.core.MultiMap headers = io.vertx.mutiny.core.MultiMap.caseInsensitiveMultiMap();
        headers.set(HttpHeaders.LOCATION, "https://qvideo.com/host123/token456/path/to/program.mpd");
        when(httpResponse.headers()).thenReturn(headers);

        // When & Then
        service.getManifestRedirectUrl(PROGRAM_ID, CHANNEL_CODE)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .assertItem("/pvr/cXZpZGVvLmNvbQ==/host123/" + PROGRAM_ID + "/a/b/c/d/e/" + CHANNEL_CODE + ".mpd");

        // Verify
        verify(cache).get(eq(PROGRAM_ID), anyCacheLoader());
        verify(stream).get("https://qvideo.com/path/to/program.mpd");
        verify(httpRequest).send();
        verifyNoMoreInteractions(cache, stream);
    }

    @Test
    @DisplayName("Should throw NotFoundException when program is missing for manifest redirect without host")
    void getManifestRedirectUrl_programNotFound_throwsException() {
        // Given
        when(cache.get(any(), anyCacheLoader())).thenReturn(mockCacheMiss());

        // When & Then
        service.getManifestRedirectUrl(PROGRAM_ID, CHANNEL_CODE)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        // Verify
        verify(cache).get(eq(PROGRAM_ID), anyCacheLoader());
        verifyNoMoreInteractions(cache, stream);
    }

    @Test
    @DisplayName("Should handle HTTP request failure and retry")
    void getManifestRedirectUrl_httpRequestFailure_retries() {
        // Given
        when(cache.get(any(), anyCacheLoader()))
        .thenReturn(mockCacheHit(
            newProgram()
                .id(PROGRAM_ID)
                .url("https://qvideo.com/path/to/program.mpd")
                .title("Test Program")
                .build()
        ));
        when(stream.get(anyString())).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send())
            .thenReturn(Uni.createFrom().failure(new RuntimeException("Network error")))
            .thenReturn(Uni.createFrom().item(httpResponse));
        io.vertx.mutiny.core.MultiMap headers2 = io.vertx.mutiny.core.MultiMap.caseInsensitiveMultiMap();
        headers2.set(HttpHeaders.LOCATION, "https://qvideo.com/host123/token456/path/to/program.mpd");
        when(httpResponse.headers()).thenReturn(headers2);

        // When & Then
        service.getManifestRedirectUrl(PROGRAM_ID, CHANNEL_CODE)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .assertItem("/pvr/cXZpZGVvLmNvbQ==/host123/" + PROGRAM_ID + "/a/b/c/d/e/" + CHANNEL_CODE + ".mpd");

        // Verify
        verify(cache).get(eq(PROGRAM_ID), anyCacheLoader());
        verify(stream).get("https://qvideo.com/path/to/program.mpd");
        verify(httpRequest, org.mockito.Mockito.times(2)).send();
        verifyNoMoreInteractions(cache, stream);
    }

    @Test
    @DisplayName("Should handle malformed location header gracefully")
    void getManifestRedirectUrl_malformedLocationHeader_handlesGracefully() {
        // Given
        when(cache.get(any(), anyCacheLoader()))
        .thenReturn(mockCacheHit(
            newProgram()
                .id(PROGRAM_ID)
                .url("https://qvideo.com/path/to/program.mpd")
                .title("Test Program")
                .build()
        ));
        when(stream.get(anyString())).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send()).thenReturn(Uni.createFrom().item(httpResponse));
        io.vertx.mutiny.core.MultiMap headers3 = io.vertx.mutiny.core.MultiMap.caseInsensitiveMultiMap();
        headers3.set(HttpHeaders.LOCATION, "invalid-url-format");
        when(httpResponse.headers()).thenReturn(headers3);

        // When & Then
        service.getManifestRedirectUrl(PROGRAM_ID, CHANNEL_CODE)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailed();

        // Verify
        verify(cache).get(eq(PROGRAM_ID), anyCacheLoader());
        verify(stream).get("https://qvideo.com/path/to/program.mpd");
        verify(httpRequest).send();
        verifyNoMoreInteractions(cache, stream);
    }
}