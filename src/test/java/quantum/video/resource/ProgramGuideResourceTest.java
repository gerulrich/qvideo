package quantum.video.resource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.video.model.ProgramGuideItem;
import quantum.video.service.ProgramGuideService;
import quantum.video.service.TokenService;
import quantum.video.utils.BaseTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ProgramGuideResourceTest extends BaseTestUtils {

    @Mock
    ProgramGuideService programGuideService;

    @Mock
    TokenService tokenService;

    @InjectMocks
    ProgramGuideResource programGuideResource;

    private ProgramGuideItem testProgramGuideItem;

    @BeforeEach
    void setUp() {
        // Create test data
        testProgramGuideItem = new ProgramGuideItem(
                "test-id-123",
                "Test Program",
                "Test program description",
                "Episode 1",
                "https://example.com/stream.mpd",
                "cover.jpg",
                "Drama",
                90,
                Instant.now().toEpochMilli(),
                Instant.now().plus(90, ChronoUnit.MINUTES).toEpochMilli()
        );
    }

    @SuppressWarnings("unchecked")
    private void mockTokenServiceSuccess() {
        when(tokenService.withToken(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<Uni<List<ProgramGuideItem>>> supplier = invocation.getArgument(0);
            return supplier.get();
        });
    }

    @Test
    @DisplayName("Should return program schedules for valid channel")
    void getProgramSchedules_shouldReturnProgramsForValidChannel() {
        // Given
        Integer channel = 123;
        List<ProgramGuideItem> expectedPrograms = List.of(testProgramGuideItem);
        
        mockTokenServiceSuccess();
        when(programGuideService.getProgramGuide(anyString(), anyString(), anyList()))
                .thenReturn(Uni.createFrom().item(expectedPrograms));

        // When
        Uni<List<ProgramGuideItem>> result = programGuideResource.getProgramSchedules(channel);

        // Then
        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(expectedPrograms);

        verify(tokenService, times(1)).withToken(any());
        verify(programGuideService, times(1)).getProgramGuide(anyString(), anyString(), eq(Collections.singletonList(channel)));
    }

    @Test
    @DisplayName("Should handle empty program list")
    void getProgramSchedules_shouldHandleEmptyProgramList() {
        // Given
        Integer channel = 456;
        List<ProgramGuideItem> emptyPrograms = Collections.emptyList();
        
        mockTokenServiceSuccess();
        when(programGuideService.getProgramGuide(anyString(), anyString(), anyList()))
                .thenReturn(Uni.createFrom().item(emptyPrograms));

        // When
        Uni<List<ProgramGuideItem>> result = programGuideResource.getProgramSchedules(channel);

        // Then
        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(emptyPrograms);

        assertTrue(emptyPrograms.isEmpty());
        verify(tokenService, times(1)).withToken(any());
        verify(programGuideService, times(1)).getProgramGuide(anyString(), anyString(), eq(Collections.singletonList(channel)));
    }

    @Test
    @DisplayName("Should use correct date range for program guide request")
    void getProgramSchedules_shouldUseCorrectDateRange() {
        // Given
        Integer channel = 789;
        List<ProgramGuideItem> expectedPrograms = List.of(testProgramGuideItem);
        ArgumentCaptor<String> dateFromCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dateToCaptor = ArgumentCaptor.forClass(String.class);
        
        mockTokenServiceSuccess();
        when(programGuideService.getProgramGuide(dateFromCaptor.capture(), dateToCaptor.capture(), anyList()))
                .thenReturn(Uni.createFrom().item(expectedPrograms));

        // When
        programGuideResource.getProgramSchedules(channel);

        // Then
        String dateFrom = dateFromCaptor.getValue();
        String dateTo = dateToCaptor.getValue();
        
        assertNotNull(dateFrom);
        assertNotNull(dateTo);
        
        // Verify that dateFrom is approximately 24 hours ago and dateTo is approximately 24 hours in the future
        long fromEpoch = Long.parseLong(dateFrom);
        long toEpoch = Long.parseLong(dateTo);
        
        // Allow some tolerance for test execution time (1 minute)
        long tolerance = 60_000L;
        long expectedFrom = Instant.now().minus(24, ChronoUnit.HOURS).toEpochMilli();
        long expectedTo = Instant.now().plus(24, ChronoUnit.HOURS).toEpochMilli();
        
        assertTrue(Math.abs(fromEpoch - expectedFrom) < tolerance, "Date from should be approximately 24 hours ago");
        assertTrue(Math.abs(toEpoch - expectedTo) < tolerance, "Date to should be approximately 24 hours in the future");
        assertTrue(toEpoch > fromEpoch, "End date should be after start date");
    }

    @Test
    @DisplayName("Should pass channel as single element list to service")
    void getProgramSchedules_shouldPassChannelAsSingleElementList() {
        // Given
        Integer channel = 999;
        
        mockTokenServiceSuccess();
        when(programGuideService.getProgramGuide(anyString(), anyString(), anyList()))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        // When
        programGuideResource.getProgramSchedules(channel);

        // Then
        verify(programGuideService, times(1)).getProgramGuide(anyString(), anyString(), eq(Collections.singletonList(channel)));
    }

    @Test
    @DisplayName("Should handle service failure")
    void getProgramSchedules_shouldHandleServiceFailure() {
        // Given
        Integer channel = 111;
        RuntimeException serviceException = new RuntimeException("Service unavailable");
        
        mockTokenServiceSuccess();
        when(programGuideService.getProgramGuide(anyString(), anyString(), anyList()))
                .thenReturn(Uni.createFrom().failure(serviceException));

        // When
        Uni<List<ProgramGuideItem>> result = programGuideResource.getProgramSchedules(channel);

        // Then
        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(RuntimeException.class, "Service unavailable");

        verify(tokenService, times(1)).withToken(any());
        verify(programGuideService, times(1)).getProgramGuide(anyString(), anyString(), eq(Collections.singletonList(channel)));
    }

    @Test
    @DisplayName("Should handle token service failure")
    void getProgramSchedules_shouldHandleTokenServiceFailure() {
        // Given
        Integer channel = 222;
        RuntimeException tokenException = new RuntimeException("Token unavailable");
        
        when(tokenService.withToken(any()))
                .thenReturn(Uni.createFrom().failure(tokenException));

        // When
        Uni<List<ProgramGuideItem>> result = programGuideResource.getProgramSchedules(channel);

        // Then
        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(RuntimeException.class, "Token unavailable");

        verify(tokenService, times(1)).withToken(any());
        verifyNoInteractions(programGuideService);
    }

    @Test
    @DisplayName("Should handle multiple programs in response")
    void getProgramSchedules_shouldHandleMultiplePrograms() {
        // Given
        Integer channel = 333;
        
        ProgramGuideItem program1 = new ProgramGuideItem(
                "prog-1", "Program 1", "Description 1", "Episode 1",
                "url1.mpd", "image1.jpg", "Comedy", 30,
                Instant.now().toEpochMilli(),
                Instant.now().plus(30, ChronoUnit.MINUTES).toEpochMilli()
        );
        
        ProgramGuideItem program2 = new ProgramGuideItem(
                "prog-2", "Program 2", "Description 2", "Episode 2",
                "url2.mpd", "image2.jpg", "Drama", 60,
                Instant.now().plus(30, ChronoUnit.MINUTES).toEpochMilli(),
                Instant.now().plus(90, ChronoUnit.MINUTES).toEpochMilli()
        );
        
        List<ProgramGuideItem> multiplePrograms = List.of(program1, program2);
        
        mockTokenServiceSuccess();
        when(programGuideService.getProgramGuide(anyString(), anyString(), anyList()))
                .thenReturn(Uni.createFrom().item(multiplePrograms));

        // When
        Uni<List<ProgramGuideItem>> result = programGuideResource.getProgramSchedules(channel);

        // Then
        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(multiplePrograms);

        List<ProgramGuideItem> returnedPrograms = result.await().indefinitely();
        assertEquals(2, returnedPrograms.size());
        assertEquals("prog-1", returnedPrograms.get(0).id());
        assertEquals("prog-2", returnedPrograms.get(1).id());
    }

    @Test
    @DisplayName("Should call withToken correctly")
    void getProgramSchedules_shouldCallWithTokenCorrectly() {
        // Given
        Integer channel = 444;
        
        when(tokenService.withToken(any()))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        // When
        programGuideResource.getProgramSchedules(channel);

        // Then
        verify(tokenService, times(1)).withToken(any());
    }

    @Test
    @DisplayName("Should handle different channel values")
    void getProgramSchedules_shouldHandleDifferentChannelValues() {
        // Given
        mockTokenServiceSuccess();
        when(programGuideService.getProgramGuide(anyString(), anyString(), anyList()))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        // Test with different channel values
        Integer[] testChannels = {1, 100, 999, 5000};
        
        for (Integer channel : testChannels) {
            // When
            Uni<List<ProgramGuideItem>> result = programGuideResource.getProgramSchedules(channel);

            // Then
            result.subscribe().withSubscriber(UniAssertSubscriber.create())
                    .assertCompleted()
                    .assertItem(Collections.emptyList());
        }

        // Verify service was called for each channel
        verify(programGuideService, times(testChannels.length))
                .getProgramGuide(anyString(), anyString(), anyList());
    }
}
