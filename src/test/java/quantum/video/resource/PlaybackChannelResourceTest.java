package quantum.video.resource;

import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.video.api.Paging;
import quantum.video.api.PlaybackChannel;
import quantum.video.builder.TestBuilder;
import quantum.video.model.PagedData;
import quantum.video.service.PlaybackChannelService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlaybackChannelResourceTest {

    @InjectMocks
    private PlaybackChannelResource resource;

    @Mock
    private PlaybackChannelService service;

    @Mock
    private SecurityContext ctx;

    @Mock
    private JWTCallerPrincipal jwt;

    @Test
    @DisplayName("Should return paged playback channels for authenticated user")
    public void testGetPlaybackChannels() {
        // Given
        when(ctx.getUserPrincipal()).thenReturn(jwt);
        when(jwt.claim(anyString())).thenReturn(Optional.of("2"));
        when(service.getPlaybackChannels(anyInt(), anyInt(), anyInt())).
        thenReturn(
            Uni.createFrom().item(
                new PagedData<>(
                    List.of(
                        Tuple2.of(
                            newChannel()
                                .id(new ObjectId("60d5f484b3f1c8b1a4e8e0a1"))
                                .name("Test Channel")
                                .logo("https://example.com/logo.png")
                                .build(),
                            newProgram()
                                .id(new ObjectId("687b2460732282022ceabe59"))
                                .title("Hello program title!")
                                .description("Program description")
                                .start(Instant.now().minus(30, ChronoUnit.MINUTES))
                                .end(Instant.now().plus(30, ChronoUnit.MINUTES))
                                .build()
                        ),
                        Tuple2.of(
                            newChannel()
                                .id(new ObjectId("60d5f484b3f1c8b1a4e8e0a2"))
                                .name("Test Channel 2")
                                .logo("https://example.com/logo2.png").build(),
                            null
                        )
                    ), 0, 5, 2)
            )
        );

        // When
        var response = resource.getPlaybackChannels(1, 5, ctx)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();

        // Then
        assertNotNull(response);
        assertEquals(Paging.of(1, 5, 2L, 1), response.paging());

        PlaybackChannel first = response.items().getFirst();
        assertEquals("60d5f484b3f1c8b1a4e8e0a1", first.id().toString());
        assertEquals("Test Channel", first.name());
        assertEquals("https://example.com/logo.png", first.logo());
        assertNull(first.drm());

        assertEquals("687b2460732282022ceabe59", first.nowPlaying().id().toString());
        assertEquals("Hello program title!", first.nowPlaying().title());
        assertEquals("Program description", first.nowPlaying().description());
        assertNotNull(first.nowPlaying().startTime());
        assertNotNull(first.nowPlaying().endTime());
        assertTrue((first.nowPlaying().startTime().isBefore(first.nowPlaying().endTime())));

        PlaybackChannel second = response.items().getLast();
        assertEquals("60d5f484b3f1c8b1a4e8e0a2", second.id().toString());
        assertEquals("Test Channel 2", second.name());
        assertEquals("https://example.com/logo2.png", second.logo());
        assertNull(second.drm());
        assertNull(second.nowPlaying());

        // Verify
        verify(service).getPlaybackChannels(2, 0, 5);
        verifyNoMoreInteractions(service);
        verify(ctx).getUserPrincipal();
        verifyNoMoreInteractions(ctx);
        verify(jwt).claim("level");
        verifyNoMoreInteractions(jwt);
    }

    @Test
    @DisplayName("Should return empty list when user is not authenticated")
    public void testGetPlaybackChannels_NotAuthenticated() {
        // Given
        when(service.getPlaybackChannels(anyInt(), anyInt(), anyInt())).
        thenReturn(
            Uni.createFrom().item(new PagedData<>(List.of(), 0,5, 0))
        );

        // When
        var response = resource.getPlaybackChannels(1, 5, ctx)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();

        // Then
        assertNotNull(response);
        assertEquals(Paging.of(1, 5, 0L, 0), response.paging());
        assertTrue(response.items().isEmpty());

        // Verify
        verify(service).getPlaybackChannels(0, 0, 5);
        verifyNoMoreInteractions(service);
        verify(ctx).getUserPrincipal();
        verifyNoMoreInteractions(ctx);
        verifyNoInteractions(jwt);
    }

    @Test
    @DisplayName("Should return playback channel with Widevine DRM license URL")
    public void testGetPlaybackChannelWithWidevineLicenseUrl() {
        // Given
        when(ctx.getUserPrincipal()).thenReturn(jwt);
        when(jwt.claim(anyString())).thenReturn(Optional.of("2"));
        when(service.getPlaybackChannel(anyString(), anyInt())).
        thenReturn(
            Uni.createFrom().item(
                Tuple2.of(
                    newChannel()
                        .id(new ObjectId("60d5f484b3f1c8b1a4e8e0a1"))
                        .name("Test Channel")
                        .logo("https://example.com/logo.png")
                        .url("http://qvideo.com/manifest/MyChannel.mpd")
                        .drm("Widevine", "http://qvideo.com/license/widevine")
                        .build(),
                    newProgram()
                        .id(new ObjectId("687b2460732282022ceabe59"))
                        .title("Hello program title!")
                        .description("Program description")
                        .start(Instant.now().minus(30, ChronoUnit.MINUTES))
                        .end(Instant.now().plus(30, ChronoUnit.MINUTES))
                        .build()
                )
            )
        );

        // When
        var play = resource.getPlaybackChannel("60d5f484b3f1c8b1a4e8e0a1", ctx)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();

        // Then
        assertNotNull(play);
        assertEquals("60d5f484b3f1c8b1a4e8e0a1", play.id().toString());
        assertEquals("Test Channel", play.name());
        assertEquals("http://qvideo.com/manifest/MyChannel.mpd", play.url());
        assertEquals("https://example.com/logo.png", play.logo());
        assertNotNull(play.drm());
        assertEquals("Widevine", play.drm().type());
        assertEquals("http://qvideo.com/license/widevine", play.drm().licenseUrl());
        assertNull(play.drm().keys());

        assertEquals("687b2460732282022ceabe59", play.nowPlaying().id().toString());
        assertEquals("Hello program title!", play.nowPlaying().title());
        assertEquals("Program description", play.nowPlaying().description());
        assertNotNull(play.nowPlaying().startTime());
        assertNotNull(play.nowPlaying().endTime());
        assertTrue((play.nowPlaying().startTime().isBefore(play.nowPlaying().endTime())));

        // Verify
        verify(service).getPlaybackChannel("60d5f484b3f1c8b1a4e8e0a1", 2);
        verifyNoMoreInteractions(service);
        verify(ctx).getUserPrincipal();
        verifyNoMoreInteractions(ctx);
        verify(jwt).claim("level");
        verifyNoMoreInteractions(jwt);
    }

    @Test
    @DisplayName("Should return playback channel with ClearKeys DRM and keys present")
    public void testGetPlaybackChannelWithClearKeys() {
        // Given
        when(ctx.getUserPrincipal()).thenReturn(jwt);
        when(jwt.claim(anyString())).thenReturn(Optional.of("3"));
        when(service.getPlaybackChannel(anyString(), anyInt())).
        thenReturn(
            Uni.createFrom().item(
                Tuple2.of(
                    newChannel()
                        .id(new ObjectId("60d5f484b3f1c8b1a4e8e0a1"))
                        .name("Test Channel")
                        .logo("https://example.com/logo.png")
                        .url("http://qvideo.com/manifest/MyChannel.mpd")
                        .drm("ClearKeys", "13151025701A69AB", "F5ACC66A6DD522D2")
                        .build(),
                    newProgram()
                        .id(new ObjectId("687b2460732282022ceabe59"))
                        .title("Hello program title!")
                        .description("Program description")
                        .start(Instant.now().minus(30, ChronoUnit.MINUTES))
                        .end(Instant.now().plus(30, ChronoUnit.MINUTES))
                        .build()
                )
            )
        );

        // When
        var play = resource.getPlaybackChannel("60d5f484b3f1c8b1a4e8e0a1", ctx)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        // Then
        assertNotNull(play);
        assertEquals("60d5f484b3f1c8b1a4e8e0a1", play.id().toString());
        assertEquals("Test Channel", play.name());
        assertEquals("http://qvideo.com/manifest/MyChannel.mpd", play.url());
        assertEquals("https://example.com/logo.png", play.logo());
        assertNotNull(play.drm());
        assertEquals("ClearKeys", play.drm().type());
        assertNull(play.drm().licenseUrl());

        assertEquals(1, play.drm().keys().size());
        assertEquals("13151025701A69AB", play.drm().keys().getFirst().kid());
        assertEquals("F5ACC66A6DD522D2", play.drm().keys().getFirst().key());

        assertEquals("687b2460732282022ceabe59", play.nowPlaying().id().toString());
        assertEquals("Hello program title!", play.nowPlaying().title());
        assertEquals("Program description", play.nowPlaying().description());
        assertNotNull(play.nowPlaying().startTime());
        assertNotNull(play.nowPlaying().endTime());
        assertTrue((play.nowPlaying().startTime().isBefore(play.nowPlaying().endTime())));

        // Verificamos las interacciones con los mocks
        verify(service).getPlaybackChannel("60d5f484b3f1c8b1a4e8e0a1", 3);
        verifyNoMoreInteractions(service);
        verify(ctx).getUserPrincipal();
        verifyNoMoreInteractions(ctx);
        verify(jwt).claim("level");
        verifyNoMoreInteractions(jwt);
    }

    @Test
    @DisplayName("Should return playback channel with ClearKeys DRM and no nowPlaying program")
    public void testGetPlaybackChannelWithoutPlayingNow() {
        // Given
        when(ctx.getUserPrincipal()).thenReturn(jwt);
        when(jwt.claim(anyString())).thenReturn(Optional.of("2"));
        when(service.getPlaybackChannel(anyString(), anyInt())).
        thenReturn(
            Uni.createFrom().item(
                Tuple2.of(
                    newChannel()
                        .id(new ObjectId("60d5f484b3f1c8b1a4e8e0a1"))
                        .name("Test Channel")
                        .url("http://qvideo.com/manifest/MyChannel.mpd")
                        .logo("https://example.com/logo.png")
                        .drm("ClearKeys", "13151025701A69AB", "F5ACC66A6DD522D2")
                        .build(),
                    null
        )));

        // When
        var play = resource.getPlaybackChannel("60d5f484b3f1c8b1a4e8e0a1", ctx)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();

        // Then
        assertNotNull(play);
        assertEquals("60d5f484b3f1c8b1a4e8e0a1", play.id().toString());
        assertEquals("Test Channel", play.name());
        assertEquals("http://qvideo.com/manifest/MyChannel.mpd", play.url());
        assertEquals("https://example.com/logo.png", play.logo());
        assertNotNull(play.drm());
        assertEquals("ClearKeys", play.drm().type());
        assertNull(play.drm().licenseUrl());

        assertEquals(1, play.drm().keys().size());
        assertEquals("13151025701A69AB", play.drm().keys().getFirst().kid());
        assertEquals("F5ACC66A6DD522D2", play.drm().keys().getFirst().key());
        assertNull(play.nowPlaying());

        // Verify
        verify(service).getPlaybackChannel("60d5f484b3f1c8b1a4e8e0a1", 2);
        verifyNoMoreInteractions(service);
        verify(ctx).getUserPrincipal();
        verifyNoMoreInteractions(ctx);
        verify(jwt).claim("level");
        verifyNoMoreInteractions(jwt);
    }

    @Test
    public void testGetPlaybackChannelProxyUrl() {
        // Given - preparamos los mocks y datos
        when(ctx.getUserPrincipal()).thenReturn(jwt);
        when(jwt.claim(anyString())).thenReturn(Optional.of("1"));
        when(service.getPlaybackChannel(anyString(), anyInt())).
        thenReturn(
            Uni.createFrom().item(
                Tuple2.of(
                    newChannel()
                        .id(new ObjectId("60d5f484b3f1c8b1a4e8e0a1"))
                        .name("Test Channel")
                        .url("http://qvideo.com/manifest/MyChannel.mpd")
                        .logo("https://example.com/logo.png")
                        .proxy(true)
                        .build(),
                    null
                )
            )
        );

        // When
        var play = resource.getPlaybackChannel("60d5f484b3f1c8b1a4e8e0a1", ctx)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();

        // Then
        assertNotNull(play);
        assertEquals("60d5f484b3f1c8b1a4e8e0a1", play.id().toString());
        assertEquals("Test Channel", play.name());
        assertEquals("http://localhost:8080/live/manifest/60d5f484b3f1c8b1a4e8e0a1.mpd", play.url());
        assertEquals("https://example.com/logo.png", play.logo());
        assertNull(play.drm());
        assertNull(play.nowPlaying());

        // Verify
        verify(service).getPlaybackChannel("60d5f484b3f1c8b1a4e8e0a1", 1);
        verifyNoMoreInteractions(service);
        verify(ctx).getUserPrincipal();
        verifyNoMoreInteractions(ctx);
        verify(jwt).claim("level");
        verifyNoMoreInteractions(jwt);
    }

    @Test
    @DisplayName("Should fail with NotFoundException when playback channel is not found")
    public void getPlaybackChannel_notFound_throwsException() {
        // Given
        when(ctx.getUserPrincipal()).thenReturn(jwt);
        when(jwt.claim(anyString())).thenReturn(Optional.of("2"));
        when(service.getPlaybackChannel(anyString(), anyInt())).
        thenReturn(Uni.createFrom().failure(new NotFoundException("Channel not found")));

        // When - ejecutamos la acción
        resource.getPlaybackChannel("60d5f484b3f1c8b1a4e8e0a1", ctx)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        // Verify
        verify(service).getPlaybackChannel("60d5f484b3f1c8b1a4e8e0a1", 2);
        verifyNoMoreInteractions(service);
        verify(ctx).getUserPrincipal();
        verifyNoMoreInteractions(ctx);
        verify(jwt).claim("level");
        verifyNoMoreInteractions(jwt);
    }

    protected TestBuilder.ChannelBuilder newChannel() {
        return new TestBuilder.ChannelBuilder();
    }

    protected TestBuilder.ProgramBuilder newProgram() {
        return new TestBuilder.ProgramBuilder();
    }

}