package quantum.video.api;

/**
 * Paging metadata for paginated API responses.
 * <p>
 * Contains information about the current page, page size, total elements, and total pages.
 * </p>
 *
 * @param page     Current page number (0-based)
 * @param size     Number of items per page
 * @param elements Total number of elements across all pages
 * @param total    Total number of pages
 */
public record Paging(int page, int size, long elements, int total) {
    public static Paging of(int page, int size, long elements, int total) {
        return new Paging(page, size, elements, total);
    }
}
