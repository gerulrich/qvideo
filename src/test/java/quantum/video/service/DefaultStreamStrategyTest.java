package quantum.video.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpClientResponse;
import jakarta.ws.rs.WebApplicationException;

import java.util.Collections;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class DefaultStreamStrategyTest {
    @Mock
    Vertx vertx;
    @Mock
    HttpClient httpClient;
    @Mock
    HttpClientRequest httpRequest;
    @Mock
    HttpClientResponse httpResponse;
    @InjectMocks
    DefaultStreamStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy.vertx = vertx;
        strategy.httpClient = httpClient;
    }

    @Test
    @DisplayName("Should create GET request with correct headers")
    void get_createsRequestWithHeaders() {
        // Given
        when(httpClient.request(any(RequestOptions.class))).thenReturn(Uni.createFrom().item(httpRequest));

        // When & Then
        String url = "https://test.com/stream.mpd";
        strategy.get(url)
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted();

        // Verify
        verify(httpClient).request(argThat(options ->
            options.getMethod() == HttpMethod.GET &&
            url.contains(options.getURI()) &&
            url.contains(options.getHost()) &&
            options.getHeaders().get("User-Agent").equals("okhttp/4.12.0") &&
            options.getHeaders().get("X-Flow-Origin").equals("AndroidTV")
        ));
        verifyNoMoreInteractions(httpClient);
        verifyNoMoreInteractions(httpRequest);
        verifyNoMoreInteractions(httpResponse);
        verifyNoMoreInteractions(vertx);
    }

    @Test
    @DisplayName("Should stream buffers when response is 200")
    void stream_successfulResponse_streamsBuffers() {
        // Given
        Buffer buffer = Buffer.buffer("data");
        when(httpClient.request(any(RequestOptions.class))).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send()).thenReturn(Uni.createFrom().item(httpResponse));
        when(httpResponse.statusCode()).thenReturn(200);
        // Simular emisión de buffer
        doAnswer(invocation -> {
            Consumer<Buffer> consumer = invocation.getArgument(0);
            consumer.accept(buffer);
            return null;
        }).when(httpResponse).handler(any());
        doAnswer(invocation -> {
            ((Runnable)invocation.getArgument(0)).run();
            return null;
        }).when(httpResponse).endHandler(any());
        doAnswer(invocation -> null).when(httpResponse).exceptionHandler(any());

        // When & Then
        String url = "https://test.com/stream.mpd";
        strategy.stream(url)
            .collect().asList()
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertCompleted()
            .assertItem(Collections.singletonList(buffer));

        // Verify
        verify(httpClient).request(any(RequestOptions.class));
        verify(httpRequest).send();
        verify(httpResponse).statusCode();
        verify(httpResponse).handler(any());
        verify(httpResponse).endHandler(any());
        verify(httpResponse).exceptionHandler(any());
        verifyNoMoreInteractions(httpClient);
        verifyNoMoreInteractions(httpRequest);
        verifyNoMoreInteractions(httpResponse);
        verifyNoMoreInteractions(vertx);
    }

    @Test
    @DisplayName("Should fail stream when response is not 200")
    void stream_non200Response_fails() {
        // Given
        when(httpClient.request(any(RequestOptions.class))).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send()).thenReturn(Uni.createFrom().item(httpResponse));
        when(httpResponse.statusCode()).thenReturn(404);

        // When & Then
        String url = "https://test.com/stream.mpd";
        strategy.stream(url)
            .collect().asList()
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(WebApplicationException.class);

        // Verify - statusCode is called 3 times in the implementation (condition + message + status)
        verify(httpClient).request(any(RequestOptions.class));
        verify(httpRequest).send();
        verify(httpResponse).statusCode();
        verifyNoMoreInteractions(httpClient);
        verifyNoMoreInteractions(httpRequest);
        verifyNoMoreInteractions(httpResponse);
        verifyNoMoreInteractions(vertx);
    }

    @Test
    @DisplayName("Should fail stream when request creation fails")
    void stream_requestCreationFails_fails() {
        // Given
        RuntimeException requestException = new RuntimeException("Request creation failed");
        when(httpClient.request(any(RequestOptions.class))).thenReturn(Uni.createFrom().failure(requestException));

        // When & Then
        String url = "https://test.com/stream.mpd";
        strategy.stream(url)
            .collect().asList()
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(RuntimeException.class);

        // Verify
        verify(httpClient).request(any(RequestOptions.class));
        verifyNoMoreInteractions(httpClient);
        verifyNoMoreInteractions(httpRequest);
        verifyNoMoreInteractions(httpResponse);
        verifyNoMoreInteractions(vertx);
    }

    @Test
    @DisplayName("Should fail stream when send fails")
    void stream_sendFails_fails() {
        // Given
        RuntimeException sendException = new RuntimeException("Send failed");
        when(httpClient.request(any(RequestOptions.class))).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send()).thenReturn(Uni.createFrom().failure(sendException));

        // When & Then
        String url = "https://test.com/stream.mpd";
        strategy.stream(url)
            .collect().asList()
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(RuntimeException.class);

        // Verify - failure at send() level will retry, but only retries the send(), not the request creation
        verify(httpClient).request(any(RequestOptions.class)); // Called once per retry attempt
        verify(httpRequest, times(4)).send(); // 1 initial + 3 retries
        verifyNoMoreInteractions(httpClient);
        verifyNoMoreInteractions(httpRequest);
        verifyNoMoreInteractions(httpResponse);
        verifyNoMoreInteractions(vertx);
    }

    @Test
    @DisplayName("Should handle exception during streaming")
    void stream_exceptionDuringStreaming_fails() {
        // Given
        RuntimeException streamException = new RuntimeException("Streaming error");
        when(httpClient.request(any(RequestOptions.class))).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send()).thenReturn(Uni.createFrom().item(httpResponse));
        when(httpResponse.statusCode()).thenReturn(200);
        doAnswer(invocation -> null).when(httpResponse).handler(any());
        doAnswer(invocation -> null).when(httpResponse).endHandler(any());
        doAnswer(invocation -> {
            java.util.function.Consumer<Throwable> consumer = invocation.getArgument(0);
            consumer.accept(streamException);
            return null;
        }).when(httpResponse).exceptionHandler(any());

        // When & Then
        String url = "https://test.com/stream.mpd";
        strategy.stream(url)
            .collect().asList()
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailedWith(RuntimeException.class);

        // Verify
        verify(httpClient).request(any(RequestOptions.class));
        verify(httpRequest).send();
        verify(httpResponse).statusCode();
        verify(httpResponse).handler(any());
        verify(httpResponse).endHandler(any());
        verify(httpResponse).exceptionHandler(any());
        verifyNoMoreInteractions(httpClient);
        verifyNoMoreInteractions(httpRequest);
        verifyNoMoreInteractions(httpResponse);
        verifyNoMoreInteractions(vertx);
    }
}
