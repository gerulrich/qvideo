package quantum.video.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.video.service.ProgramManifestService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ProgramManifestResourceTest {

    @InjectMocks
    private ProgramManifestResource resource;

    @Mock
    private ProgramManifestService service;

    @Test
    public void testRedirectWithValidChannel() {
        when(service.getManifestRedirectUrl(anyString(), anyString())).
        thenReturn(
            Uni.createFrom().item("http://qvideo.com/MyChannel.mpd")
        );

        Response response = resource.redirect("60d5f484b3f1c8b1a4e8e0a1", "MyChannel")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals("http://qvideo.com/MyChannel.mpd", response.getLocation().toString());

        verify(service).getManifestRedirectUrl("60d5f484b3f1c8b1a4e8e0a1", "MyChannel");
        verifyNoMoreInteractions(service);
    }

    @Test
    public void testRedirectWithInValidChannel() {
        when(service.getManifestRedirectUrl(anyString(), anyString())).
        thenReturn(
            Uni.createFrom().nullItem()
        );

        resource.redirect("60d5f484b3f1c8b1a4e8e0a1", "MyChannel")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);

        verify(service).getManifestRedirectUrl("60d5f484b3f1c8b1a4e8e0a1", "MyChannel");
        verifyNoMoreInteractions(service);
    }

    @Test
    public void testGetManifest() {
        when(service.getManifestRedirectUrl(anyString(), anyString(), anyString(), anyString())).
        thenReturn(
            Uni.createFrom().item("http://qvideo.com/MyChannel.mpd")
        );

        when(service.stream(anyString())).
        thenReturn(
            Multi.createFrom().items(
                Buffer.buffer("<?xml version=\"1.0\"?>"),
                Buffer.buffer("<MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\">"),
                Buffer.buffer("</MPD>")
            )
        );

        resource.manifest("qvideo.com", "myToken", "60d5f484b3f1c8b1a4e8e0a1", "MyChannel")
                .subscribe()
                .withSubscriber(AssertSubscriber.create(3))
                .assertItems(new Buffer[] {
                        Buffer.buffer("<?xml version=\"1.0\"?>"),
                        Buffer.buffer("<MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\">"),
                        Buffer.buffer("</MPD>")
                })
                .assertCompleted();

        verify(service).getManifestRedirectUrl("qvideo.com", "myToken", "60d5f484b3f1c8b1a4e8e0a1", "MyChannel");
        verify(service).stream("http://qvideo.com/MyChannel.mpd");
        verifyNoMoreInteractions(service);
    }

    @Test
    public void testGetManifestInvalidChannel() {
        when(service.getManifestRedirectUrl(anyString(), anyString(), anyString(), anyString())).
        thenReturn(
            Uni.createFrom().nullItem()
        );

        resource.manifest("qvideo.com", "myToken", "60d5f484b3f1c8b1a4e8e0a1", "MyChannel")
                .subscribe()
                .withSubscriber(AssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);


        verify(service).getManifestRedirectUrl("qvideo.com", "myToken", "60d5f484b3f1c8b1a4e8e0a1", "MyChannel");
        verifyNoMoreInteractions(service);
    }

}