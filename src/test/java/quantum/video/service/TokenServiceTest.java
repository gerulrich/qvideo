package quantum.video.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import quantum.video.client.SessionClient;
import quantum.video.client.TokenClient;
import quantum.video.utils.BaseTestUtils;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest extends BaseTestUtils {
    
    @Mock
    SessionClient sessionClient;
    @Mock
    TokenClient tokenClient;
    @InjectMocks
    TokenService tokenService;

    @BeforeEach
    void setUp() {
        // Given
        setPrivateField(tokenService, "flowAccessToken", "flow-access-token");
        setPrivateField(tokenService, "deviceToken", "device-token");
        setPrivateField(tokenService, "casId", "cas-id");
        setPrivateField(tokenService, "mac", "mac-address");
        setPrivateField(tokenService, "deviceUuid", "device-uuid");
        setPrivateField(tokenService, "deviceAppVersion", "app-version");
        setPrivateField(tokenService, "account", "account-id");
    }

    @Test
    @DisplayName("Should return cached token if valid")
    void getToken_returnsCachedTokenIfValid() {
        // Given
        setPrivateField(tokenService, "currentToken", "cached-token");
        setPrivateField(tokenService, "expiresAt", Instant.now().plusSeconds(1000));

        // When & Then
        tokenService.getToken()
            .subscribe()
            .withSubscriber(io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create())
            .assertCompleted()
            .assertItem("cached-token");

        // Verify
        verifyNoInteractions(sessionClient, tokenClient);
    }

    @Test
    @DisplayName("Should request new token if current token is null")
    void getToken_tokenIsNull_requestsNewToken() {
        // Given
        setPrivateField(tokenService, "currentToken", null);
        setPrivateField(tokenService, "expiresAt", Instant.now().plusSeconds(1000));

        var responseMock = Mockito.mock(Response.class);
        Mockito.when(responseMock.getHeaderString("Authorization")).thenReturn("Bearer session123");
        Mockito.when(sessionClient.session(anyString(), any())).thenReturn(Uni.createFrom().item(responseMock));

        var jsonMock = Mockito.mock(JsonObject.class);
        Mockito.when(jsonMock.getString("token_mamushka")).thenReturn("new-token");
        Mockito.when(tokenClient.token(any(), anyString())).thenReturn(Uni.createFrom().item(jsonMock));

        // When & Then
        tokenService.getToken()
            .subscribe()
            .withSubscriber(io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create())
            .assertCompleted()
            .assertItem("new-token");

        // Verify
        verify(sessionClient).session(anyString(), any());
        verify(tokenClient).token(argThat( request -> request.token().equals("session123")), any());
        verifyNoMoreInteractions(sessionClient, tokenClient);
    }

    @Test
    @DisplayName("Should renew token if expired")
    void getToken_tokenExpired_renewsToken() {
        // Given
        setPrivateField(tokenService, "currentToken", "expired-token");
        setPrivateField(tokenService, "expiresAt", Instant.now().minusSeconds(1000));

        var responseMock = Mockito.mock(Response.class);
        Mockito.when(responseMock.getHeaderString("Authorization")).thenReturn("Bearer session123");
        Mockito.when(sessionClient.session(anyString(), any())).thenReturn(Uni.createFrom().item(responseMock));

        var jsonMock = Mockito.mock(JsonObject.class);
        Mockito.when(jsonMock.getString("token_mamushka")).thenReturn("new-token");
        Mockito.when(tokenClient.token(any(), anyString())).thenReturn(Uni.createFrom().item(jsonMock));

        // When & Then
        tokenService.getToken()
            .subscribe()
            .withSubscriber(io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create())
            .assertCompleted()
            .assertItem("new-token");

        // Verify
        verify(sessionClient).session(anyString(), any());
        verify(tokenClient).token(argThat( request -> request.token().equals("session123")), any());
        verifyNoMoreInteractions(sessionClient, tokenClient);
    }

    @Test
    @DisplayName("Should return current cached token")
    void getCachedToken_returnsCurrentToken() {
        // Given
        BaseTestUtils.setPrivateField(tokenService, "currentToken", "token123");

        // When & Then
        assertEquals("token123", tokenService.getCachedToken());

        // Verify
        verifyNoInteractions(sessionClient, tokenClient);
    }

    @Test
    @DisplayName("Should execute supplier with valid token")
    void withToken_executesSupplierWithValidToken() {
        // Given
        BaseTestUtils.setPrivateField(tokenService, "currentToken", "token123");
        BaseTestUtils.setPrivateField(tokenService, "expiresAt", Instant.now().plusSeconds(1000));
        Supplier<Uni<String>> supplier = () -> Uni.createFrom().item("result");

        // When & Then
        tokenService.withToken(supplier)
            .subscribe()
            .withSubscriber(io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create())
            .assertCompleted()
            .assertItem("result");

        // Verify
        verifyNoInteractions(sessionClient, tokenClient);
    }
}

