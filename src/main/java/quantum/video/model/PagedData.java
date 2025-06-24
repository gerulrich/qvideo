package quantum.video.model;

import java.util.List;

public record PagedData<T>(
    List<T> items,
    int page,
    int size,
    long elements,
    int total
) {
    public PagedData(
        List<T> items,
        int page,
        int size,
        long elements
    ) {
        this(items, page, size, elements, (int) Math.ceil((double) elements / size));
    }

    public PagedData(
            int page,
            int size,
            long elements
    ) {
        this(List.of(), page, size, elements, (int) Math.ceil((double) elements / size));
    }

}