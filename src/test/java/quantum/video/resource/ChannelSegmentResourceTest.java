package quantum.video.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.video.service.ChannelSegmentService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelSegmentResourceTest {

    @InjectMocks
    ChannelSegmentResource resource;

    @Mock
    ChannelSegmentService service;

    @Test
    @DisplayName("Should stream audio segment for valid request")
    public void getAudioSegment_streamsAudio() {
        // Given
        when(service.getAudioSegment(anyString(), anyString(), anyString(), anyString())).
        thenReturn(
            Uni.createFrom().item("https://qvideo.com/segment/00001.mp4")
        );

        when(service.stream(anyString())).
        thenReturn(
            Multi.createFrom().items(
                Buffer.buffer("Ãx9¶*~"),
                Buffer.buffer("þ@¢¢¥Ç"),
                Buffer.buffer("µ#øZ!")
            )
        );

        // When & Then
        resource.audio("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel")
            .subscribe()
            .withSubscriber(AssertSubscriber.create(3))
            .assertItems(new Buffer[] {
                Buffer.buffer("Ãx9¶*~"),
                Buffer.buffer("þ@¢¢¥Ç"),
                Buffer.buffer("µ#øZ!")
            })
            .assertCompleted();

        // Verify
        verify(service).getAudioSegment("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel");
        verify(service).stream("https://qvideo.com/segment/00001.mp4");
        verifyNoMoreInteractions(service);
    }

    @Test
    @DisplayName("Should fail with NotFoundException for missing audio segment")
    public void getAudioSegment_notFound_throwsException() {
        // Given
        when(service.getAudioSegment(anyString(), anyString(), anyString(), anyString())).
        thenReturn(
            Uni.createFrom().failure(new NotFoundException("Channel Segment not found"))
        );

        // When & Then
        resource.audio("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel")
            .subscribe()
            .withSubscriber(AssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        // Verify
        verify(service).getAudioSegment("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel");
        verifyNoMoreInteractions(service);
    }

    @Test
    @DisplayName("Should stream video segment for valid request")
    public void getVideoSegment_streamsVideo() {
        // Given
        when(service.getVideoSegment(anyString(), anyString(), anyString(), anyString())).
        thenReturn(
            Uni.createFrom().item("https://qvideo.com/segment/00001.mp4")
        );

        when(service.stream(anyString())).
        thenReturn(
            Multi.createFrom().items(
                Buffer.buffer("¢Ý~µ@"),
                Buffer.buffer("Õß#§ô"),
                Buffer.buffer("½çøÿ?")
            )
        );

        // When & Then
        resource.video("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel")
            .subscribe()
            .withSubscriber(AssertSubscriber.create(3))
            .assertItems(new Buffer[] {
                Buffer.buffer("¢Ý~µ@"),
                Buffer.buffer("Õß#§ô"),
                Buffer.buffer("½çøÿ?")
            })
            .assertCompleted();

        // Verify
        verify(service).getVideoSegment("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel");
        verify(service).stream("https://qvideo.com/segment/00001.mp4");
        verifyNoMoreInteractions(service);
    }

    @Test
    @DisplayName("Should fail with NotFoundException for missing video segment")
    public void getVideoSegment_notFound_throwsException() {
        // Given
        when(service.getVideoSegment(anyString(), anyString(), anyString(), anyString())).
        thenReturn(
            Uni.createFrom().failure(new NotFoundException("Video segment not found"))
        );

        // When & Then
        resource.video("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel")
            .subscribe()
            .withSubscriber(AssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        // Verify
        verify(service).getVideoSegment("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel");
        verifyNoMoreInteractions(service);
    }

}