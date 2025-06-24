package quantum.video.api;

public record Paging(int page, int size, long elements, int total) {
    public static Paging of(int page, int size, long elements, int total) {
        return new Paging(page, size, elements, total);
    }
}

