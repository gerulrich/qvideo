package quantum.video.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.video.service.TokenService;
import quantum.video.utils.BaseTestUtils;

@ExtendWith(MockitoExtension.class)
class ApiHeadersFactoryTest extends BaseTestUtils {

    @Mock
    TokenService tokenService;

    @InjectMocks
    ApiHeadersFactory apiHeadersFactory;

    @BeforeEach
    void setUp() {
        // Mock token service to return a test token
        when(tokenService.getCachedToken()).thenReturn("test-auth-token");
    }

    @Test
    @DisplayName("Should add required headers to outgoing request")
    void update_shouldAddRequiredHeaders() {
        MultivaluedMap<String, String> result = apiHeadersFactory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());
        assertNotNull(result);
        assertEquals("Bearer test-auth-token", result.getFirst("Authorization"));
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertEquals("okhttp/4.11.0", result.getFirst("User-Agent"));
    }

    @Test
    @DisplayName("Should handle null incoming headers")
    void update_shouldHandleNullIncomingHeaders() {
        MultivaluedMap<String, String> result = apiHeadersFactory.update(null, new MultivaluedHashMap<>());
        assertNotNull(result);
        assertEquals("Bearer test-auth-token", result.getFirst("Authorization"));
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertEquals("okhttp/4.11.0", result.getFirst("User-Agent"));
    }

    @Test
    @DisplayName("Should handle null client outgoing headers")
    void update_shouldHandleNullClientOutgoingHeaders() {
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.add("Custom-Header", "custom-value");
        MultivaluedMap<String, String> result = apiHeadersFactory.update(incomingHeaders, null);
        assertNotNull(result);
        assertEquals("Bearer test-auth-token", result.getFirst("Authorization"));
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertEquals("okhttp/4.11.0", result.getFirst("User-Agent"));
    }

    @Test
    @DisplayName("Should handle null token from token service")
    void update_shouldHandleNullToken() {
        when(tokenService.getCachedToken()).thenReturn(null);
        MultivaluedMap<String, String> result = apiHeadersFactory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());
        assertNotNull(result);
        assertEquals("Bearer null", result.getFirst("Authorization"));
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertEquals("okhttp/4.11.0", result.getFirst("User-Agent"));
    }

    @Test
    @DisplayName("Should handle empty string token from token service")
    void update_shouldHandleEmptyToken() {
        when(tokenService.getCachedToken()).thenReturn("");
        MultivaluedMap<String, String> result = apiHeadersFactory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());
        assertNotNull(result);
        assertEquals("Bearer ", result.getFirst("Authorization"));
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertEquals("okhttp/4.11.0", result.getFirst("User-Agent"));
    }

    @Test
    @DisplayName("Should always create new headers map and not modify input")
    void update_shouldCreateNewHeadersMap() {
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.add("Incoming-Header", "incoming-value");
        MultivaluedMap<String, String> clientOutgoingHeaders = new MultivaluedHashMap<>();
        clientOutgoingHeaders.add("Client-Header", "client-value");

        // When
        MultivaluedMap<String, String> result = apiHeadersFactory.update(incomingHeaders, clientOutgoingHeaders);

        // Then
        assertNotNull(result);
        assertNotSame(incomingHeaders, result);
        assertNotSame(clientOutgoingHeaders, result);
        
        // Verify original maps are not modified
        assertFalse(incomingHeaders.containsKey("Authorization"));
        assertFalse(clientOutgoingHeaders.containsKey("Authorization"));
        
        // Verify result contains only the expected headers
        assertEquals(3, result.size()); // Authorization, Content-Type, User-Agent
        assertTrue(result.containsKey("Authorization"));
        assertTrue(result.containsKey("Content-Type"));
        assertTrue(result.containsKey("User-Agent"));
    }

    @Test
    @DisplayName("Should call token service getCachedToken method")
    void update_shouldCallTokenService() {
        // Given
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> clientOutgoingHeaders = new MultivaluedHashMap<>();

        // When
        apiHeadersFactory.update(incomingHeaders, clientOutgoingHeaders);

        // Then
        verify(tokenService, times(1)).getCachedToken();
        verifyNoMoreInteractions(tokenService);
    }

    @Test
    @DisplayName("Should return consistent header keys")
    void update_shouldReturnConsistentHeaderKeys() {
        // Given
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> clientOutgoingHeaders = new MultivaluedHashMap<>();

        // When
        MultivaluedMap<String, String> result = apiHeadersFactory.update(incomingHeaders, clientOutgoingHeaders);

        // Then
        assertTrue(result.containsKey("Authorization"));
        assertTrue(result.containsKey("Content-Type"));
        assertTrue(result.containsKey("User-Agent"));
        assertEquals(3, result.size());
    }
}
