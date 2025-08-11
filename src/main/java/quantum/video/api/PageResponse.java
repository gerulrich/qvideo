package quantum.video.api;

import java.util.List;

/**
 * Generic paginated response wrapper for API list endpoints.
 * <p>
 * Encapsulates a list of items and paging metadata for client-side navigation.
 * </p>
 *
 * @param items  List of items in the current page
 * @param paging Paging metadata (page, size, total, etc.)
 * @param <T>    Type of the items in the list
 */
public record PageResponse<T> (
    List<T> items,
    Paging paging
){
}
