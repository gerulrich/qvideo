package quantum.video.model;

import java.util.List;

/**
 * Generic container for paginated data results.
 * <p>
 * This record provides a standardized structure for returning paginated data across
 * the application. It contains both the actual data items for the current page and
 * the pagination metadata necessary for client-side pagination controls.
 * </p>
 * <p>
 * PagedData is used primarily by service layer components to return structured paginated
 * results to resource classes, which then typically convert these to API-specific
 * pagination response DTOs.
 * </p>
 * <p>
 * The record supports generic typing to accommodate different types of paginated entities.
 * </p>
 *
 * @param <T> The type of items contained in the paginated result
 * @param items The list of items for the current page
 * @param page The current page number (0-based)
 * @param size The requested page size (items per page)
 * @param elements The total number of elements across all pages
 * @param total The total number of pages based on the page size
 */
public record PagedData<T>(
    List<T> items,
    int page,
    int size,
    long elements,
    int total
) {
    /**
     * Constructor for PagedData with automatic calculation of the total page count.
     * <p>
     * This constructor calculates the total number of pages based on the total number
     * of elements and the page size.
     * </p>
     *
     * @param items The list of items for the current page
     * @param page The current page number (0-based)
     * @param size The requested page size (items per page)
     * @param elements The total number of elements across all pages
     */
    public PagedData(
        List<T> items,
        int page,
        int size,
        long elements
    ) {
        this(items, page, size, elements, (int) Math.ceil((double) elements / size));
    }

    /**
     * Constructor for empty PagedData with pagination metadata.
     * <p>
     * This constructor is useful for cases where no items are available for the
     * current page but pagination metadata is still required.
     * </p>
     *
     * @param page The current page number (0-based)
     * @param size The requested page size (items per page)
     * @param elements The total number of elements across all pages
     */
    public PagedData(
            int page,
            int size,
            long elements
    ) {
        this(List.of(), page, size, elements, (int) Math.ceil((double) elements / size));
    }

}