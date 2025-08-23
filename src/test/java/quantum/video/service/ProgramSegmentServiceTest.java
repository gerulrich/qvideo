package quantum.video.service;

import io.quarkus.cache.Cache;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.video.model.Program;
import quantum.video.utils.BaseTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramSegmentServiceTest extends BaseTestUtils {

    private static final String DOMAIN_BASE64 = "cXZpZGVvLmNvbQ=="; // Base64 encoding of "qvideo.com"
    private static final String PROGRAM_ID = "66ed71d0174ce2b912555115";

    @InjectMocks
    private ProgramSegmentService service;
    @Mock
    private Cache cache;


    @Test
    @DisplayName("Should return video segment URL when program is found")
    void testGetVideoUrl_ProgramFound() {
        // Given
        Program program = new Program();
        program.url = "https://qvideo.com/path/to/ch1.mpd";
        when(cache.get(anyString(), anyCacheLoader())).thenReturn(mockCacheHit(program));

        // When & Then
        service.getVideoSegment(DOMAIN_BASE64, "token123", PROGRAM_ID, "seg1")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem("https://qvideo.com/token123/path/to/ch1-avc1_seg1.mp4");

        // Verify
        verify(cache).get(eq(PROGRAM_ID), anyCacheLoader());
        verifyNoMoreInteractions(cache);
    }

    @Test
    @DisplayName("Should throw NotFoundException when program is missing for video segment request")
    void testGetVideoUrl_ProgramNotFound() {
        // Given
        when(cache.get(anyString(), anyCacheLoader())).thenReturn(mockCacheMiss());

        // When & Then
        service.getVideoSegment(DOMAIN_BASE64, "token123", PROGRAM_ID, "seg1")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);

        // Verify
        verify(cache).get(eq(PROGRAM_ID), anyCacheLoader());
        verifyNoMoreInteractions(cache);
    }

    @Test
    @DisplayName("Should return audio segment URL when program is found")
    void testGetAudioUrl_ProgramFound() {
        // Given
        Program program = new Program();
        program.url = "https://domain.com/path/to/ch1.mpd";
        when(cache.get(anyString(), anyCacheLoader())).thenReturn(mockCacheHit(program));

        // When & Then
        service.getAudioSegment(DOMAIN_BASE64, "token123", PROGRAM_ID, "seg1")
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .assertItem("https://qvideo.com/token123/path/to/ch1-mp4a_seg1.mp4");

        // Verify
        verify(cache).get(eq(PROGRAM_ID), any());
        verifyNoMoreInteractions(cache);
    }

    @Test
    @DisplayName("Should throw NotFoundException when program is missing for audio segment request")
    void testGetAudioUrl_ProgramNotFound() {
        // Given
        when(cache.get(anyString(), anyCacheLoader())).thenReturn(mockCacheMiss());

        // When & Then
        service.getAudioSegment(DOMAIN_BASE64, "token123", PROGRAM_ID, "seg1")
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        // Verify
        verify(cache).get(eq(PROGRAM_ID), any());
        verifyNoMoreInteractions(cache);
    }
}
