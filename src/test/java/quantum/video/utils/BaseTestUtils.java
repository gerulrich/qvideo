package quantum.video.utils;

import static org.mockito.ArgumentMatchers.any;
import java.util.function.Function;
import io.smallrye.mutiny.Uni;
import quantum.video.builder.HeadersBuilder;
import quantum.video.builder.TestBuilder;

public abstract class BaseTestUtils {
    /**
     * Set a private field value using reflection.
     * @param target The object whose field to set
     * @param fieldName The name of the field
     * @param value The value to set
     */
    protected static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Error setting field '" + fieldName + "'", e);
        }
    }

    /**
     * Get a private field value using reflection.
     * @param target The object whose field to get
     * @param fieldName The name of the field
     * @return The value of the field
     */
    protected static Object getPrivateField(Object target, String fieldName) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Error getting field '" + fieldName + "'", e);
        }
    }

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
