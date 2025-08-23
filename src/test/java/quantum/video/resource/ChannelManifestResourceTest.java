package quantum.video.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.video.service.ChannelManifestService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelManifestResourceTest {

    public static final ObjectId CHANNEL_ID = new ObjectId("60d5f484b3f1c8b1a4e8e0a1");
    @InjectMocks
    private ChannelManifestResource resource;

    @Mock
    private ChannelManifestService service;

    @Test
    @DisplayName("Should redirect with valid channel")
    public void shouldRedirectWithValidChannel() {
        // Given
        when(service.getManifestRedirectUrl(any())).
        thenReturn(
            Uni.createFrom().item("http://qvideo.com/MyChannel.mpd")
        );

        // When
        Response response = resource.redirect(CHANNEL_ID)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();

        // Then
        assertEquals("http://qvideo.com/MyChannel.mpd", response.getLocation().toString());

        // Verify
        verify(service).getManifestRedirectUrl(CHANNEL_ID);
        verifyNoMoreInteractions(service);
    }

    @Test
    @DisplayName("Should fail redirect with invalid channel")
    public void shouldFailRedirectWithInvalidChannel() {
        // Given
        when(service.getManifestRedirectUrl(any())).
        thenReturn(
            Uni.createFrom().failure(new NotFoundException("Manifest not found"))
        );

        // When & Then
        resource.redirect(CHANNEL_ID)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        // Verify
        verify(service).getManifestRedirectUrl(CHANNEL_ID);
        verifyNoMoreInteractions(service);
    }

    @Test
    @DisplayName("Should get manifest for valid channel")
    public void shouldGetManifestForValidChannel() {
        // Given
        when(service.getManifestUrl(anyString(), anyString(), any())).
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

        // When & Then
        resource.manifest("qvideo.com", "myToken", CHANNEL_ID, "MyChannel")
            .subscribe()
            .withSubscriber(AssertSubscriber.create(3))
            .assertItems(new Buffer[] {
                Buffer.buffer("<?xml version=\"1.0\"?>"),
                Buffer.buffer("<MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\">"),
                Buffer.buffer("</MPD>")
            })
            .assertCompleted();

        // Verify
        verify(service).getManifestUrl("qvideo.com", "myToken", CHANNEL_ID);
        verify(service).stream("http://qvideo.com/MyChannel.mpd");
        verifyNoMoreInteractions(service);
    }

    @Test
    @DisplayName("Should fail to get manifest for invalid channel")
    public void shouldFailGetManifestForInvalidChannel() {
        // Given
        when(service.getManifestUrl(anyString(), anyString(), any())).
        thenReturn(
            Uni.createFrom().failure(new NotFoundException("Manifest not found"))
        );

        // When & Then
        resource.manifest("qvideo.com", "myToken", CHANNEL_ID, "MyChannel")
            .subscribe()
            .withSubscriber(AssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        // Verify
        verify(service).getManifestUrl("qvideo.com", "myToken", CHANNEL_ID);
        verifyNoMoreInteractions(service);
    }

}