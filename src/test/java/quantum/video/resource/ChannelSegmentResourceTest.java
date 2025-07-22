package quantum.video.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.ws.rs.NotFoundException;
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
    public void testGetAudioSegment() {
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

        resource.audio("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel")
            .subscribe()
            .withSubscriber(AssertSubscriber.create(3))
            .assertItems(new Buffer[] {
                Buffer.buffer("Ãx9¶*~"),
                Buffer.buffer("þ@¢¢¥Ç"),
                Buffer.buffer("µ#øZ!")
            })
            .assertCompleted();

        verify(service).getAudioSegment("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel");
        verify(service).stream("https://qvideo.com/segment/00001.mp4");
        verifyNoMoreInteractions(service);
    }

    @Test
    public void testGetAudioSegmentNotFound() {
        when(service.getAudioSegment(anyString(), anyString(), anyString(), anyString())).
        thenReturn(
            Uni.createFrom().nullItem()
        );

        resource.audio("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel")
            .subscribe()
            .withSubscriber(AssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        verify(service).getAudioSegment("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel");
        verifyNoMoreInteractions(service);
    }

    @Test
    public void testGetVideoSegment() {
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

        resource.video("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel")
            .subscribe()
            .withSubscriber(AssertSubscriber.create(3))
            .assertItems(new Buffer[] {
                Buffer.buffer("¢Ý~µ@"),
                Buffer.buffer("Õß#§ô"),
                Buffer.buffer("½çøÿ?")
            })
            .assertCompleted();

        verify(service).getVideoSegment("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel");
        verify(service).stream("https://qvideo.com/segment/00001.mp4");
        verifyNoMoreInteractions(service);
    }

    @Test
    public void testGetVideoSegmentNotFound() {
        when(service.getVideoSegment(anyString(), anyString(), anyString(), anyString())).
        thenReturn(
            Uni.createFrom().nullItem()
        );

        resource.video("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel")
            .subscribe()
            .withSubscriber(AssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        verify(service).getVideoSegment("cXZpZGVvLmNvbQ==", "myToken", "MyChanel", "MyChannel");
        verifyNoMoreInteractions(service);
    }

}