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
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import quantum.video.utils.BaseTestUtils;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class ChannelSegmentServiceTest extends BaseTestUtils {

    private static final String DOMAIN_BASE64 = "cXZpZGVvLmNvbQ=="; // Base64 encoding of "qvideo.com"
    public static final ObjectId CHANNEL_ID = new ObjectId("60d5f484b3f1c8b1a4e8e0a1");

    @InjectMocks
    private ChannelSegmentService service;
    @Mock
    private Cache cache;

    @Test
    @DisplayName("Should return video segment URL when channel is found")
    void getVideoSegmentFound_returnsUrl() {
        // Given
        when(cache.get(any(), anyCacheLoader()))
        .thenReturn(mockCacheHit(
            newChannel()
                .id(CHANNEL_ID)
                .url("https://qvideo.com/path/to/ch1.mpd")
                .build()
        ));

        // When & Then
        service.getVideoSegment(DOMAIN_BASE64, "token123", CHANNEL_ID, "seg1")
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .assertItem("https://qvideo.com/token123/path/to/ch1-avc1_seg1.mp4");

        // Verify
        verify(cache).get(eq(CHANNEL_ID), anyCacheLoader());
        verifyNoMoreInteractions(cache);
    }

    @Test
    @DisplayName("Should throw NotFoundException when channel is missing for video segment request")
    void getVideoSegmentNotFound_throwsException() {
        // Given
        when(cache.get(any(), anyCacheLoader())).thenReturn(mockCacheMiss());

        // When & Then
        service.getVideoSegment(DOMAIN_BASE64, "token123", CHANNEL_ID, "seg1")
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        // Verify
        verify(cache).get(eq(CHANNEL_ID), anyCacheLoader());
        verifyNoMoreInteractions(cache);
    }

    @Test
    @DisplayName("Should return audio segment URL when channel is found")
    void testGetAudioUrl_ChannelFound() {
        // Given
        when(cache.get(any(), anyCacheLoader()))
        .thenReturn(mockCacheHit(
            newChannel()
                .id(CHANNEL_ID)
                .url("https://domain.com/path/to/ch1.mpd")
                .build()
        ));

        // When & Then
        service.getAudioSegment(DOMAIN_BASE64, "token123", CHANNEL_ID,"seg1")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem("https://qvideo.com/token123/path/to/ch1-mp4a_seg1.mp4");

        // Verify
        verify(cache).get(eq(CHANNEL_ID), anyCacheLoader());
        verifyNoMoreInteractions(cache);
    }

    @Test
    @DisplayName("Should throw NotFoundException when channel is missing for audio segment request")
    void testGetAudioUrl_ChannelNotFound() {
        // Given
        when(cache.get(any(), anyCacheLoader())).thenReturn(mockCacheMiss());

        // When & Then
        service.getAudioSegment(DOMAIN_BASE64, "token123", CHANNEL_ID,"seg1")
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        // Verify
        verify(cache).get(eq(CHANNEL_ID), anyCacheLoader());
        verifyNoMoreInteractions(cache);
    }
}