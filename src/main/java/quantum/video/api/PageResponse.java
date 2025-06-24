package quantum.video.api;

import java.util.List;

public record PageResponse<T> (
    List<T> items,
    Paging paging
){
}
