package quantum.video.resource;

import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
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
import quantum.video.api.PlaybackProgram;
import quantum.video.builder.TestBuilder;
import quantum.video.model.PagedData;
import quantum.video.service.ProgramService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaybackProgramResourceTest {

    @InjectMocks
    PlaybackProgramResource resource;

    @Mock
    ProgramService service;

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
        when(service.getPrograms(anyInt(), anyInt(), anyInt())).
        thenReturn(
            Uni.createFrom().item(
                new PagedData<>(
                    List.of(
                        newProgram()
                            .id(new ObjectId("60d5f484b3f1c8b1a4e8e0a1"))
                            .title("Hello program title!")
                            .description("Program description")
                            .start(Instant.now().minus(30, ChronoUnit.MINUTES))
                            .end(Instant.now().plus(30, ChronoUnit.MINUTES))
                            .build(),
                        newProgram()
                            .id(new ObjectId("687b2460732282022ceabe59"))
                            .title("Hello another program title!")
                            .description("Another program description")
                            .start(Instant.now().minus(30, ChronoUnit.MINUTES))
                            .end(Instant.now().plus(30, ChronoUnit.MINUTES))
                            .build()
                        ), 0, 5, 2)
            )
        );

        // When
        var response = resource.getPlaybackPrograms(1, 5, ctx)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();

        // Then
        assertNotNull(response);
        assertEquals(Paging.of(1, 5, 2L, 1), response.paging());

        PlaybackProgram first = response.items().getFirst();
        assertEquals("60d5f484b3f1c8b1a4e8e0a1", first.id().toString());
        assertEquals("Hello program title!", first.title());
        assertEquals("Program description", first.description());
        assertNull(first.drm());

        assertNotNull(first.startTime());
        assertNotNull(first.endTime());
        assertTrue((first.startTime().isBefore(first.endTime())));

        PlaybackProgram second = response.items().getLast();
        assertEquals("687b2460732282022ceabe59", second.id().toString());
        assertEquals("Hello another program title!", second.title());
        assertEquals("Another program description", second.description());
        assertNull(second.drm());

        // Verify
        verify(service).getPrograms(2, 0, 5);
        verifyNoMoreInteractions(service);
        verify(ctx).getUserPrincipal();
        verifyNoMoreInteractions(ctx);
        verify(jwt).claim("level");
        verifyNoMoreInteractions(jwt);
    }

    @Test
    @DisplayName("Should return playback program for valid id")
    void getPlaybackProgram_returnsProgram() {
        // Given
        when(ctx.getUserPrincipal()).thenReturn(jwt);
        when(jwt.claim(anyString())).thenReturn(Optional.of("2"));
        when(service.getProgram(anyString(), anyInt())).
        thenReturn(
            Uni.createFrom().item(
                newProgram()
                    .id(new ObjectId("60d5f484b3f1c8b1a4e8e0a1"))
                    .title("Program Title")
                    .description("Program Description")
                    .start(Instant.now().minus(30, ChronoUnit.MINUTES))
                    .end(Instant.now().plus(30, ChronoUnit.MINUTES))
                    .build()
            )
        );

        // When
        PlaybackProgram result = resource.getPlaybackProgram("60d5f484b3f1c8b1a4e8e0a1", ctx)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();

        // Then
        assertNotNull(result);
        assertEquals("60d5f484b3f1c8b1a4e8e0a1", result.id().toString());
        assertEquals("Program Title", result.title());
        assertEquals("Program Description", result.description());

        // Verify
        verify(service).getProgram("60d5f484b3f1c8b1a4e8e0a1", 2);
        verifyNoMoreInteractions(service);
        verify(ctx).getUserPrincipal();
        verifyNoMoreInteractions(ctx);
        verify(jwt).claim("level");
        verifyNoMoreInteractions(jwt);
    }

    @Test
    @DisplayName("Should fail with NotFoundException for invalid id")
    void getPlaybackProgram_notFound_throwsException() {
        // Given
        when(ctx.getUserPrincipal()).thenReturn(jwt);
        when(jwt.claim(anyString())).thenReturn(Optional.of("1"));
        when(service.getProgram(anyString(), anyInt())).
        thenReturn(
            Uni.createFrom().failure(new NotFoundException("Program not found"))
        );

        // When & Then
        resource.getPlaybackProgram("60d5f484b3f1c8b1a4e8e0a1", ctx)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(NotFoundException.class);

        // Verify
        verify(service).getProgram("60d5f484b3f1c8b1a4e8e0a1", 1);
        verifyNoMoreInteractions(service);
        verifyNoMoreInteractions(service);
        verify(ctx).getUserPrincipal();
        verifyNoMoreInteractions(ctx);
        verify(jwt).claim("level");
        verifyNoMoreInteractions(jwt);
    }

    protected TestBuilder.ProgramBuilder newProgram() {
        return new TestBuilder.ProgramBuilder();
    }
}