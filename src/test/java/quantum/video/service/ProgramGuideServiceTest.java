package quantum.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.video.client.ApiClient;
import quantum.video.model.ProgramGuideItem;
import quantum.video.utils.BaseTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ProgramGuideServiceTest extends BaseTestUtils {

    @Mock
    ApiClient apiClient;

    @InjectMocks
    ProgramGuideService programGuideService;

    private JsonArray testJsonResponse;
    private JsonObject testProgramJson;

    @BeforeEach
    void setUp() {
        // Create test JSON data
        testProgramJson = new JsonObject()
                .put("id", "test-program-123")
                .put("title", "Test Program")
                .put("description", "Test program description")
                .put("episodeTitle", "Episode 1")
                .put("genre", "Drama")
                .put("duration", 90)
                .put("startTime", Instant.now().toEpochMilli())
                .put("endTime", Instant.now().plusSeconds(5400).toEpochMilli()) // 90 minutes later
                .put("resources", new JsonArray()
                        .add(new JsonObject()
                                .put("protocol", "DASH")
                                .put("encryption", "Widevine")
                                .put("url", "https://example.com/stream.mpd")))
                .put("images", new JsonArray()
                        .add(new JsonObject()
                                .put("usage", "BROWSE")
                                .put("suffix", "cover")
                                .put("format", "jpg")));

        testJsonResponse = new JsonArray().add(new JsonArray().add(testProgramJson));
    }

    @Test
    @DisplayName("Should return program guide items for valid request")
    void getProgramGuide_shouldReturnProgramGuideItems() {
        // Given
        String dateFrom = "1640995200000";
        String dateTo = "1641081600000";
        List<Integer> channels = List.of(123);

        when(apiClient.search(dateFrom, dateTo, channels))
                .thenReturn(Uni.createFrom().item(testJsonResponse));

        // When
        Uni<List<ProgramGuideItem>> result = programGuideService.getProgramGuide(dateFrom, dateTo, channels);

        // Then
        List<ProgramGuideItem> items = result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(1, items.size());
        ProgramGuideItem item = items.getFirst();
        assertEquals("test-program-123", item.id());
        assertEquals("Test Program", item.title());
        assertEquals("Test program description", item.description());
        assertEquals("Episode 1", item.episode());
        assertEquals("Drama", item.genre());
        assertEquals(90, item.duration());
        assertEquals("https://example.com/stream.mpd", item.url());
        assertEquals("cover.jpg", item.image());
        assertNotNull(item.start());
        assertNotNull(item.end());

        verify(apiClient, times(1)).search(dateFrom, dateTo, channels);
    }

    @Test
    @DisplayName("Should handle empty response")
    void getProgramGuide_shouldHandleEmptyResponse() {
        // Given
        String dateFrom = "1640995200000";
        String dateTo = "1641081600000";
        List<Integer> channels = List.of(456);
        JsonArray emptyResponse = new JsonArray().add(new JsonArray());

        when(apiClient.search(dateFrom, dateTo, channels))
                .thenReturn(Uni.createFrom().item(emptyResponse));

        // When
        List<ProgramGuideItem> items = programGuideService.getProgramGuide(dateFrom, dateTo, channels)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .getItem();

        assertTrue(items.isEmpty());

        verify(apiClient, times(1)).search(dateFrom, dateTo, channels);
    }

    @Test
    @DisplayName("Should handle multiple program items")
    void getProgramGuide_shouldHandleMultipleProgramItems() {
        // Given
        String dateFrom = "1640995200000";
        String dateTo = "1641081600000";
        List<Integer> channels = List.of(789);

        JsonObject program = new JsonObject()
            .put("id", "test-program-456")
            .put("title", "Second Program")
            .put("description", "Second program description")
            .put("episodeTitle", "Episode 2")
            .put("genre", "Comedy")
            .put("duration", 60)
            .put("startTime", Instant.now().plusSeconds(5400).toEpochMilli())
            .put("endTime", Instant.now().plusSeconds(9000).toEpochMilli())
            .put("resources", new JsonArray()
                .add(new JsonObject()
                    .put("protocol", "DASH")
                    .put("encryption", "Widevine")
                    .put("url", "https://example.com/stream2.mpd")))
                .put("images", new JsonArray()
                    .add(new JsonObject()
                    .put("usage", "BROWSE")
                    .put("suffix", "cover2")
                    .put("format", "png")));

        JsonArray multipleItemsResponse = new JsonArray()
        .add(new JsonArray().add(testProgramJson).add(program));

        when(apiClient.search(dateFrom, dateTo, channels))
        .thenReturn(Uni.createFrom().item(multipleItemsResponse));

        // When
        Uni<List<ProgramGuideItem>> result = programGuideService.getProgramGuide(dateFrom, dateTo, channels);

        // Then
        List<ProgramGuideItem> items = result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(2, items.size());
        
        ProgramGuideItem first = items.getFirst();
        assertEquals("test-program-123", first.id());
        assertEquals("Test Program", first.title());
        
        ProgramGuideItem second = items.get(1);
        assertEquals("test-program-456", second.id());
        assertEquals("Second Program", second.title());
        assertEquals("Comedy", second.genre());

        verify(apiClient, times(1)).search(dateFrom, dateTo, channels);
    }

    @Test
    @DisplayName("Should handle multiple channels")
    void getProgramGuide_shouldHandleMultipleChannels() {
        // Given
        String dateFrom = "1640995200000";
        String dateTo = "1641081600000";
        List<Integer> channels = List.of(111, 222, 333);

        when(apiClient.search(dateFrom, dateTo, channels))
                .thenReturn(Uni.createFrom().item(testJsonResponse));

        // When
        programGuideService.getProgramGuide(dateFrom, dateTo, channels);

        // Then
        verify(apiClient, times(1)).search(eq(dateFrom), eq(dateTo), eq(channels));
    }

    @Test
    @DisplayName("Should pass correct parameters to ApiClient")
    void getProgramGuide_shouldPassCorrectParameters() {
        // Given
        String dateFrom = "1609459200000";
        String dateTo = "1609545600000";
        List<Integer> channels = List.of(999);

        when(apiClient.search(dateFrom, dateTo, channels))
                .thenReturn(Uni.createFrom().item(testJsonResponse));

        // When
        programGuideService.getProgramGuide(dateFrom, dateTo, channels);

        // Then
        verify(apiClient, times(1)).search(dateFrom, dateTo, channels);
    }

    @Test
    @DisplayName("Should handle null values in JSON gracefully")
    void getProgramGuide_shouldHandleNullValuesInJson() {
        // Given
        String dateFrom = "1640995200000";
        String dateTo = "1641081600000";
        List<Integer> channels = List.of(123);

        JsonObject programWithNulls = new JsonObject()
                .put("id", "test-program-null")
                .put("title", "Program with nulls")
                .putNull("description")
                .putNull("episodeTitle")
                .put("genre", "Unknown")
                .putNull("duration")
                .put("startTime", Instant.now().toEpochMilli())
                .put("endTime", Instant.now().plusSeconds(3600).toEpochMilli())
                .put("resources", new JsonArray())
                .put("images", new JsonArray());

        JsonArray responseWithNulls = new JsonArray().add(new JsonArray().add(programWithNulls));

        when(apiClient.search(dateFrom, dateTo, channels))
                .thenReturn(Uni.createFrom().item(responseWithNulls));

        // When
        Uni<List<ProgramGuideItem>> result = programGuideService.getProgramGuide(dateFrom, dateTo, channels);

        // Then
        List<ProgramGuideItem> items = result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertEquals(1, items.size());
        ProgramGuideItem item = items.get(0);
        assertEquals("test-program-null", item.id());
        assertEquals("Program with nulls", item.title());
        assertNull(item.description());
        assertNull(item.episode());
        assertEquals("Unknown", item.genre());
        assertNull(item.duration());
        assertNull(item.url()); // Empty resources array
        assertNull(item.image()); // Empty images array

        verify(apiClient, times(1)).search(dateFrom, dateTo, channels);
    }

    @Test
    @DisplayName("Should handle single channel list")
    void getProgramGuide_shouldHandleSingleChannelList() {
        // Given
        String dateFrom = "1640995200000";
        String dateTo = "1641081600000";
        List<Integer> channels = Collections.singletonList(777);

        when(apiClient.search(dateFrom, dateTo, channels))
                .thenReturn(Uni.createFrom().item(testJsonResponse));

        // When
        programGuideService.getProgramGuide(dateFrom, dateTo, channels);

        // Then
        verify(apiClient, times(1)).search(dateFrom, dateTo, Collections.singletonList(777));
    }

    @Test
    @DisplayName("Should transform JSON response correctly")
    void getProgramGuide_shouldTransformJsonResponseCorrectly() {
        // Given
        String dateFrom = "1640995200000";
        String dateTo = "1641081600000";
        List<Integer> channels = List.of(123);

        when(apiClient.search(dateFrom, dateTo, channels))
                .thenReturn(Uni.createFrom().item(testJsonResponse));

        // When
        Uni<List<ProgramGuideItem>> result = programGuideService.getProgramGuide(dateFrom, dateTo, channels);

        // Then
        List<ProgramGuideItem> items = result.await().indefinitely();
        
        assertNotNull(items);
        assertEquals(1, items.size());
        
        ProgramGuideItem item = items.get(0);
        // Verify all fields were transformed correctly
        assertNotNull(item.id());
        assertNotNull(item.title());
        assertNotNull(item.description());
        assertNotNull(item.episode());
        assertNotNull(item.genre());
        assertNotNull(item.duration());
        assertNotNull(item.url());
        assertNotNull(item.image());
        assertNotNull(item.start());
        assertNotNull(item.end());
        
        verify(apiClient, times(1)).search(dateFrom, dateTo, channels);
    }
}
