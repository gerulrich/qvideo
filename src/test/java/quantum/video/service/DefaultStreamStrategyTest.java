package quantum.video.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vertx.core.http.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpClientResponse;
import jakarta.ws.rs.WebApplicationException;

import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class DefaultStreamStrategyTest {
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
            .assertCompleted()
            .assertItem(httpRequest);

        // Verify
        verify(httpClient).request(argThat(options ->
            options.getMethod() == HttpMethod.GET &&
            url.contains(options.getURI()) &&
            url.contains(options.getHost()) &&
            options.getHeaders().get("User-Agent").equals("okhttp/4.12.0") &&
            options.getHeaders().get("X-Flow-Origin").equals("AndroidTV")
        ));
        verifyNoMoreInteractions(httpClient, httpRequest, httpResponse);
    }

    @Test
    @DisplayName("Should stream buffers when response is 200")
    void stream_successfulResponse_streamsBuffers() {
        // Given
        Buffer buffer = Buffer.buffer("data");
        when(httpClient.request(any(RequestOptions.class))).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send()).thenReturn(Uni.createFrom().item(httpResponse));
        when(httpResponse.statusCode()).thenReturn(200);

        // Mock the streaming handlers
        doAnswer(invocation -> {
            Consumer<Buffer> handler = invocation.getArgument(0);
            handler.accept(buffer);
            return httpResponse;
        }).when(httpResponse).handler(any());

        doAnswer(invocation -> {
            Runnable endHandler = invocation.getArgument(0);
            endHandler.run();
            return httpResponse;
        }).when(httpResponse).endHandler(any());

        when(httpResponse.exceptionHandler(any())).thenReturn(httpResponse);
        when(httpRequest.setChunked(true)).thenReturn(httpRequest);
        when(httpRequest.idleTimeout(30000)).thenReturn(httpRequest);
        when(httpResponse.pause()).thenReturn(httpResponse);

        // When & Then
        String url = "https://test.com/stream.mpd";
        AssertSubscriber<Buffer> subscriber = strategy.stream(url)
            .subscribe().withSubscriber(AssertSubscriber.create(1));

        subscriber
            .assertCompleted()
            .assertItems(buffer);

        // Verify
        verify(httpClient).request(any(RequestOptions.class));
        verify(httpRequest).setChunked(true);
        verify(httpRequest).idleTimeout(30000);
        verify(httpRequest).send();
        verify(httpResponse).statusCode();
        verify(httpResponse).handler(any());
        verify(httpResponse).endHandler(any());
        verify(httpResponse).exceptionHandler(any());
        verify(httpResponse).pause(); // Called by onTermination handler
        verifyNoMoreInteractions(httpClient, httpRequest, httpResponse);
    }

    @Test
    @DisplayName("Should fail stream when response is not 200")
    void stream_non200Response_fails() {
        // Given
        when(httpClient.request(any(RequestOptions.class))).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send()).thenReturn(Uni.createFrom().item(httpResponse));
        when(httpResponse.statusCode()).thenReturn(404);
        when(httpRequest.setChunked(true)).thenReturn(httpRequest);
        when(httpRequest.idleTimeout(30000)).thenReturn(httpRequest);

        // When & Then
        String url = "https://test.com/stream.mpd";
        AssertSubscriber<Buffer> subscriber = strategy.stream(url)
            .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.assertFailedWith(WebApplicationException.class, "Failed: 404");

        // Verify
        verify(httpClient).request(any(RequestOptions.class));
        verify(httpRequest).setChunked(true);
        verify(httpRequest).idleTimeout(30000);
        verify(httpRequest).send();
        verify(httpResponse).statusCode();
        verifyNoMoreInteractions(httpClient, httpRequest, httpResponse);
    }

    @Test
    @DisplayName("Should fail stream when request creation fails")
    void stream_requestCreationFails_fails() {
        // Given
        RuntimeException requestException = new RuntimeException("Request creation failed");
        when(httpClient.request(any(RequestOptions.class))).thenReturn(Uni.createFrom().failure(requestException));

        // When & Then
        String url = "https://test.com/stream.mpv";
        AssertSubscriber<Buffer> subscriber = strategy.stream(url)
            .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.assertFailedWith(RuntimeException.class, "Request creation failed");

        // Verify
        verify(httpClient).request(any(RequestOptions.class));
        verifyNoInteractions(httpRequest);
        verifyNoInteractions(httpResponse);
        verifyNoMoreInteractions(httpClient);
    }

    @Test
    @DisplayName("Should retry when send fails and eventually fail")
    void stream_sendFails_retriesAndFails() {
        // Given
        when(httpClient.request(any(RequestOptions.class))).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send()).thenReturn(Uni.createFrom().failure(new RuntimeException("Send failed")));
        when(httpRequest.setChunked(true)).thenReturn(httpRequest);
        when(httpRequest.idleTimeout(30000)).thenReturn(httpRequest);

        // When & Then
        String url = "https://test.com/stream.mpd";
        strategy.stream(url)
            .subscribe()
            .withSubscriber(AssertSubscriber.create())
            .assertFailedWith(RuntimeException.class, "Send failed");

        // Verify - should retry 3 times (initial + 3 retries = 4 total)
        verify(httpClient).request(any(RequestOptions.class));
        verify(httpRequest, times(4)).setChunked(true);
        verify(httpRequest, times(4)).idleTimeout(30000);
        verify(httpRequest, times(4)).send();
        verifyNoInteractions(httpResponse);
        verifyNoMoreInteractions(httpClient, httpRequest);
    }

    @Test
    @DisplayName("Should handle exception during streaming")
    void stream_exceptionDuringStreaming_fails() {
        // Given
        RuntimeException streamException = new RuntimeException("Streaming error");
        when(httpClient.request(any(RequestOptions.class))).thenReturn(Uni.createFrom().item(httpRequest));
        when(httpRequest.send()).thenReturn(Uni.createFrom().item(httpResponse));
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpRequest.setChunked(true)).thenReturn(httpRequest);
        when(httpRequest.idleTimeout(30000)).thenReturn(httpRequest);
        when(httpResponse.pause()).thenReturn(httpResponse);

        when(httpResponse.handler(any())).thenReturn(httpResponse);
        when(httpResponse.endHandler(any())).thenReturn(httpResponse);

        doAnswer(invocation -> {
            Consumer<Throwable> exceptionHandler = invocation.getArgument(0);
            exceptionHandler.accept(streamException);
            return httpResponse;
        }).when(httpResponse).exceptionHandler(any());

        // When & Then
        String url = "https://test.com/stream.mpd";
        strategy.stream(url)
            .subscribe()
            .withSubscriber(AssertSubscriber.create())
            .assertFailedWith(RuntimeException.class, "Streaming error");

        // Verify
        verify(httpClient).request(any(RequestOptions.class));
        verify(httpRequest).setChunked(true);
        verify(httpRequest).idleTimeout(30000);
        verify(httpRequest).send();
        verify(httpResponse).statusCode();
        verify(httpResponse).handler(any());
        verify(httpResponse).endHandler(any());
        verify(httpResponse).exceptionHandler(any());
        verify(httpResponse).pause();
        verifyNoMoreInteractions(httpClient, httpRequest, httpResponse);
    }
}
