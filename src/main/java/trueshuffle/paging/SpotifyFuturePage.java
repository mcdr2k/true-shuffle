package trueshuffle.paging;

import trueshuffle.requests.exceptions.FatalRequestResponse;

public class SpotifyFuturePage<T> {
    private transient final SpotifyPageConsumer<T> consumer;

    private final int offset;
    private final int limit;

    public SpotifyFuturePage(SpotifyPageConsumer<T> consumer) {
        this(consumer, 0);
    }

    public SpotifyFuturePage(SpotifyPageConsumer<T> consumer, int offset) {
        this(consumer, offset, 50);
    }

    public SpotifyFuturePage(SpotifyPageConsumer<T> consumer, int offset, int limit) {
        this.consumer = consumer;
        this.offset = offset;
        this.limit = limit;
    }

    public SpotifyPage<T> execute() throws FatalRequestResponse {
        return new SpotifyPage<>(consumer, consumer.consumePage(offset, limit));
    }
}
