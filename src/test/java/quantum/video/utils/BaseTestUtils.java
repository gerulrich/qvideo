package quantum.video.utils;

import io.smallrye.mutiny.Uni;
import quantum.video.builder.HeadersBuilder;
import quantum.video.builder.TestBuilder;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;

public abstract class BaseTestUtils {

    /**
     * Matcher for any cache loader function (Function<String, Uni<?>>).
     * Useful for mocking cache.get calls with any loader.
     */
    @SuppressWarnings("unchecked")
    protected <K, V> Function<K, Uni<V>> anyCacheLoader() {
        return any(Function.class);
    }

    /**
     * Simulates a cache hit: cache returns a Uni<Uni<V>> with a value.
     */
    protected <V> Uni<Uni<V>> mockCacheHit(V value) {
        return Uni.createFrom().item(Uni.createFrom().item(value));
    }

    /**
     * Simulates a cache miss: cache returns a Uni<Uni<V>> with null.
     */
    protected <V> Uni<Uni<V>> mockCacheMiss() {
        return Uni.createFrom().item(Uni.createFrom().nullItem());
    }

    protected TestBuilder.ChannelBuilder newChannel() {
        return new TestBuilder.ChannelBuilder();
    }

    protected TestBuilder.ProgramBuilder newProgram() {
        return new TestBuilder.ProgramBuilder();
    }

    protected HeadersBuilder newHeaders() {
        return new HeadersBuilder();
    }
}
